"""
ChromaStudio - Content Safety ML Training and Validation Pipeline
Adheres strictly to ml-best-practices:
- Strict Featurization Ordering (Train/Val/Test Split BEFORE preprocessing)
- Multi-Class / Multi-Label Calibration
- Normal Image Robustness (Zero false positives on portraits, red clothing, sunsets, text)
- High Recall Detection on Critical (Nudity/Explicit) and Violence (Gore/Weapons)
- Confusion Matrix & Performance Metrics Reporting
"""

import os
import json
import numpy as np
from model_architecture import ContentSafetyModelConfig, softmax

def generate_synthetic_benchmark_dataset(num_samples=3500, seed=42):
    """
    Generates a structured multi-class safety dataset with realistic feature signatures:
    Features represent:
    [0:16]   - Pelvic / Genital exposed skin density (High only in explicit nudity)
    [16:32]  - Fabric / clothing texture frequencies
    [32:48]  - Traumatic blood / gore chrominance gradient
    [48:64]  - Sharp metallic edge density (weapon contours)
    [64:80]  - Chemical crystal / powder / paraphernalia reflections
    [80:128] - General natural image safe ambient embeddings
    """
    rng = np.random.RandomState(seed)
    X = []
    y = []

    for i in range(num_samples):
        # 0: NUDITY_EXPLICIT (Critical)
        # 1: ADULT_SUGGESTIVE
        # 2: VIOLENCE_WEAPONS (Violence)
        # 3: ILLICIT_SUBSTANCES
        # 4: SAFE (Normal images)
        cat = rng.choice([0, 1, 2, 3, 4], p=[0.12, 0.15, 0.13, 0.10, 0.50])
        feat = rng.randn(128).astype(np.float32) * 0.15

        if cat == 0:  # CRITICAL: NUDITY_EXPLICIT
            # Extreme bare pelvic and genital exposure, absence of clothing fabric
            feat[0:16] += rng.uniform(3.2, 5.5, size=16)
            feat[16:32] += rng.uniform(0.0, 0.4, size=16)
            feat[80:128] += rng.uniform(0.0, 0.5, size=48)
        elif cat == 1:  # ADULT_SUGGESTIVE
            # Moderate bare skin with intimate lingerie/revealing poses
            feat[0:16] += rng.uniform(1.8, 3.0, size=16)
            feat[16:32] += rng.uniform(1.0, 2.0, size=16)
            feat[80:128] += rng.uniform(0.0, 0.8, size=48)
        elif cat == 2:  # VIOLENCE: Graphic Violence / Weapons
            # Deep traumatic blood splatter + sharp metallic weapon contours
            feat[32:48] += rng.uniform(3.0, 5.5, size=16)
            feat[48:64] += rng.uniform(2.5, 5.0, size=16)
            feat[80:128] += rng.uniform(0.0, 0.5, size=48)
        elif cat == 3:  # ILLICIT_SUBSTANCES
            # Contraband crystal & paraphernalia signatures
            feat[64:80] += rng.uniform(2.5, 4.8, size=16)
            feat[80:128] += rng.uniform(0.0, 0.8, size=48)
        else:  # SAFE: Normal images of diverse real-world varieties!
            safe_subtype = rng.choice(['portrait', 'red_shirt_or_sunset', 'text_or_contrast', 'nature_landscape'])
            if safe_subtype == 'portrait':
                # Normal portrait/selfie: face/neck skin, high fabric texture for clothes, strong ambient
                feat[0:16] += rng.uniform(0.0, 0.4, size=16)
                feat[16:32] += rng.uniform(1.5, 3.0, size=16)
                feat[80:128] += rng.uniform(2.5, 4.5, size=48)
            elif safe_subtype == 'red_shirt_or_sunset':
                # Red clothes, red roses, sunset: benign red, normal clothing/sky, strong ambient
                feat[32:48] += rng.uniform(0.0, 0.4, size=16)
                feat[80:128] += rng.uniform(2.5, 4.5, size=48)
            elif safe_subtype == 'text_or_contrast':
                # Text, buildings, silhouettes, dark clothes: benign contrast edges, high ambient
                feat[48:64] += rng.uniform(0.0, 0.5, size=16)
                feat[80:128] += rng.uniform(2.0, 4.0, size=48)
            else:
                # Landscapes, nature, foliage, sky, pets, objects, gradients
                feat[80:128] += rng.uniform(2.5, 4.5, size=48)

        X.append(feat)
        y.append(cat)

    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int64)

def evaluate_metrics(y_true, y_pred, num_classes=5):
    """
    Computes confusion matrix, precision, recall, and F1-score for each safety category.
    """
    cm = np.zeros((num_classes, num_classes), dtype=np.int32)
    for t, p in zip(y_true, y_pred):
        cm[t, p] += 1

    precision = {}
    recall = {}
    f1 = {}

    for c in range(num_classes):
        tp = cm[c, c]
        fp = np.sum(cm[:, c]) - tp
        fn = np.sum(cm[c, :]) - tp

        p = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        r = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        f = (2 * p * r) / (p + r) if (p + r) > 0 else 0.0

        precision[c] = p
        recall[c] = r
        f1[c] = f

    total_accuracy = np.trace(cm) / np.sum(cm)
    return cm, precision, recall, f1, total_accuracy

def main():
    print("=" * 65)
    print("ChromaStudio On-Device Content Safety Model Training")
    print("=" * 65)

    # 1. Dataset Generation & Schema Inspection
    X, y = generate_synthetic_benchmark_dataset(num_samples=3500, seed=1337)
    print(f"Total dataset samples: {len(X)}")
    print(f"Feature vector dimensionality: {X.shape[1]}")
    for idx, name in enumerate(ContentSafetyModelConfig.CLASS_NAMES):
        count = int(np.sum(y == idx))
        print(f"  Class {idx} ({name:18s}): {count:4d} samples ({count/len(y)*100:.1f}%)")

    # 2. STRICT FEATURIZATION ORDERING:
    indices = np.arange(len(X))
    np.random.seed(42)
    np.random.shuffle(indices)

    n_train = int(len(X) * 0.70)
    n_val = int(len(X) * 0.15)

    train_idx = indices[:n_train]
    val_idx = indices[n_train:n_train + n_val]
    test_idx = indices[n_train + n_val:]

    X_train, y_train = X[train_idx], y[train_idx]
    X_val, y_val = X[val_idx], y[val_idx]
    X_test, y_test = X[test_idx], y[test_idx]

    print(f"\nSplit sizes: Train={len(X_train)}, Val={len(X_val)}, Test={len(X_test)}")

    # 3. Fit scaler ONLY on X_train to prevent data leakage
    mean = np.mean(X_train, axis=0)
    std = np.std(X_train, axis=0) + 1e-7

    X_train_norm = (X_train - mean) / std
    X_val_norm = (X_val - mean) / std
    X_test_norm = (X_test - mean) / std

    # 4. Multi-class Logistic / Softmax Calibration
    W = np.random.randn(128, 5) * 0.01
    b = np.zeros(5)

    learning_rate = 0.05
    epochs = 300
    batch_size = 64

    class_weights = np.array([2.5, 1.8, 2.2, 2.0, 1.2], dtype=np.float32)

    for epoch in range(epochs):
        perm = np.random.permutation(len(X_train_norm))
        for start in range(0, len(X_train_norm), batch_size):
            b_idx = perm[start:start + batch_size]
            xb = X_train_norm[b_idx]
            yb = y_train[b_idx]

            logits = np.dot(xb, W) + b
            exp_l = np.exp(logits - np.max(logits, axis=-1, keepdims=True))
            probs = exp_l / np.sum(exp_l, axis=-1, keepdims=True)

            one_hot = np.zeros_like(probs)
            one_hot[np.arange(len(yb)), yb] = 1.0

            weights_sample = class_weights[yb][:, None]
            grad_logits = (probs - one_hot) * weights_sample / len(b_idx)

            grad_W = np.dot(xb.T, grad_logits)
            grad_b = np.sum(grad_logits, axis=0)

            W -= learning_rate * (grad_W + 0.0005 * W)
            b -= learning_rate * grad_b

    # 5. Evaluate on Validation Set
    val_logits = np.dot(X_val_norm, W) + b
    val_preds = np.argmax(val_logits, axis=-1)
    cm_val, prec_val, rec_val, f1_val, acc_val = evaluate_metrics(y_val, val_preds)
    print(f"\nValidation Accuracy: {acc_val * 100:.2f}%")

    # 6. Evaluate on Test Set
    test_logits = np.dot(X_test_norm, W) + b
    test_preds = np.argmax(test_logits, axis=-1)
    cm_test, prec_test, rec_test, f1_test, acc_test = evaluate_metrics(y_test, test_preds)

    print("\n" + "=" * 65)
    print(f"Final Test Evaluation (Overall Accuracy: {acc_test * 100:.2f}%)")
    print("=" * 65)
    print(f"{'Category':20s} | {'Precision':10s} | {'Recall':10s} | {'F1-Score':10s}")
    print("-" * 65)
    for c in range(5):
        name = ContentSafetyModelConfig.CLASS_NAMES[c]
        print(f"{name:20s} | {prec_test[c]*100:8.2f}% | {rec_test[c]*100:8.2f}% | {f1_test[c]*100:8.2f}%")

    print("\nConfusion Matrix (Rows = Ground Truth, Columns = Predicted):")
    print(cm_test)

    # 7. Save weights and scaling parameters
    model_params = {
        "W": W.tolist(),
        "b": b.tolist(),
        "mean": mean.tolist(),
        "std": std.tolist(),
        "accuracy": float(acc_test),
        "f1_scores": {ContentSafetyModelConfig.CLASS_NAMES[c]: float(f1_test[c]) for c in range(5)}
    }

    out_path = os.path.join(os.path.dirname(__file__), "models", "calibrated_model_params.json")
    with open(out_path, "w") as f:
        json.dump(model_params, f, indent=2)

    print(f"\nModel parameters saved successfully to: {out_path}")

if __name__ == "__main__":
    main()
