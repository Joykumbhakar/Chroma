const messageInput = document.getElementById('messageInput');
const attachBtn = document.getElementById('attachBtn');
const attachmentMenu = document.getElementById('attachmentMenu');
const actionBtn = document.getElementById('actionBtn');
const actionIcon = document.getElementById('actionIcon');
const recordingOverlay = document.getElementById('recordingOverlay');
const recordTimerEl = document.getElementById('recordTimer');
const audioPreview = document.getElementById('audioPreview');
const messagesContainer = document.getElementById('messagesContainer');
const welcomeScreen = document.getElementById('welcomeScreen');
const chatArea = document.getElementById('chatArea');
const contextOverlay = document.getElementById('contextOverlay');
const selectionHeader = document.getElementById('selectionHeader');

let isSelectionMode = false;
let selectedMessages = new Set();
let recordingInterval;
let recordSeconds = 0;
let hasAudioToPreview = false;

// Audio context helper for Apple sounds
function playSound(type) {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        if(type === 'send') {
            osc.type = 'sine';
            osc.frequency.setValueAtTime(587.33, now);
            osc.frequency.exponentialRampToValueAtTime(880, now + 0.12);
            gain.gain.setValueAtTime(0.2, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
            osc.start(now);
            osc.stop(now + 0.25);
        }
    } catch(e) {}
}

const iconMic = document.getElementById('iconMic');
const iconSend = document.getElementById('iconSend');

messageInput.addEventListener('input', () => {
    const text = messageInput.innerText.trim();
    if (text.length > 0) {
        iconMic.classList.add('hidden');
        iconSend.classList.remove('hidden');
        iconSend.classList.add('flex');
    } else {
        if (!hasAudioToPreview) {
            iconSend.classList.add('hidden');
            iconSend.classList.remove('flex');
            iconMic.classList.remove('hidden');
            iconMic.classList.add('flex');
        }
    }
});

attachBtn.addEventListener('click', () => {
    attachmentMenu.classList.toggle('attachment-menu-open');
    attachBtn.querySelector('svg').classList.toggle('plus-icon-rotate');
});

document.addEventListener('click', (e) => {
    if (!attachBtn.contains(e.target) && !attachmentMenu.contains(e.target)) {
        attachmentMenu.classList.remove('attachment-menu-open');
        attachBtn.querySelector('svg').classList.remove('plus-icon-rotate');
    }
});

let recordPressTimer;
let isRecording = false;

actionBtn.addEventListener('pointerdown', (e) => {
    if (messageInput.innerText.trim().length > 0 || hasAudioToPreview) return;
    
    e.preventDefault();
    recordPressTimer = setTimeout(() => {
        startRecording();
    }, 200);
});

actionBtn.addEventListener('pointerup', (e) => {
    if (messageInput.innerText.trim().length > 0) {
        sendTextMessage(messageInput.innerText);
        messageInput.innerText = '';
        messageInput.dispatchEvent(new Event('input'));
        return;
    }

    if (hasAudioToPreview) {
        sendAudioMessage();
        cancelAudio();
        return;
    }

    if (isRecording) {
        stopRecording();
    } else {
        clearTimeout(recordPressTimer);
    }
});

actionBtn.addEventListener('pointerleave', () => {
    if (isRecording) stopRecording();
    clearTimeout(recordPressTimer);
});

function startRecording() {
    isRecording = true;
    recordSeconds = 0;
    recordTimerEl.innerText = '0:00';
    recordingOverlay.classList.remove('opacity-0', 'pointer-events-none');
    
    recordingInterval = setInterval(() => {
        recordSeconds++;
        const m = Math.floor(recordSeconds / 60);
        const s = recordSeconds % 60;
        recordTimerEl.innerText = `${m}:${s.toString().padStart(2, '0')}`;
    }, 1000);
}

function stopRecording() {
    isRecording = false;
    clearInterval(recordingInterval);
    recordingOverlay.classList.add('opacity-0', 'pointer-events-none');
    
    if (recordSeconds > 0) {
        showAudioPreview();
    }
}

function showAudioPreview() {
    hasAudioToPreview = true;
    audioPreview.classList.remove('translate-y-10', 'opacity-0', 'pointer-events-none');
    iconMic.classList.add('hidden');
    iconSend.classList.remove('hidden');
    iconSend.classList.add('flex');
}

window.cancelAudio = function() {
    hasAudioToPreview = false;
    audioPreview.classList.add('translate-y-10', 'opacity-0', 'pointer-events-none');
    messageInput.dispatchEvent(new Event('input'));
}

window.sendQuickMessage = function(text) {
    sendTextMessage(text);
};

function sendTextMessage(text) {
    if (!text.trim()) return;
    playSound('send');
    
    welcomeScreen.style.display = 'none';
    messagesContainer.style.display = 'flex';

    const msgId = 'msg-' + Date.now();
    let replyBlock = '';
    if (isReplying && replyMessageData) {
        replyBlock = `
            <div style="background: rgba(0,0,0,0.1); border-left: 3px solid #ffffff; padding: 4px 8px; border-radius: 4px; margin-bottom: 6px; font-size: 12px; opacity: 0.9;">
                <div style="font-weight: 600; margin-bottom: 2px;">${replyMessageData.isThem ? 'Sarah Jenkins' : 'You'}</div>
                <div style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px;">${replyMessageData.text}</div>
            </div>
        `;
        cancelReply();
    }

    const msgHTML = `
        <div class="msg-bubble-row me fade-in" id="${msgId}" data-text="${text}" style="position: relative;">
            <input type="checkbox" class="msg-checkbox accent-blue-500" onchange="toggleSelect('${msgId}')">
            <div class="msg-bubble" style="display:flex; flex-direction:column; align-items:flex-end; gap:6px;">
                ${replyBlock}
                <div style="display:flex; align-items:center; gap:6px; width:100%;">
                    <div style="flex: 1;">${text}</div>
                    <div style="display: flex; align-items: center; gap: 4px; margin-bottom: -4px;">
                        <span style="font-size: 10px; color: rgba(255,255,255,0.7);">${new Date().toLocaleTimeString('en-US', {hour: 'numeric', minute: '2-digit'})}</span>
                        <span class="msg-status-receipt" style="display:flex; align-items:center; height: 16px;"></span>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    messagesContainer.insertAdjacentHTML('beforeend', msgHTML);
    setupLongPress(document.getElementById(msgId));
    updateMessageStatus(msgId);
    
    setTimeout(() => {
        chatArea.scrollTo({ top: chatArea.scrollHeight, behavior: 'smooth' });
    }, 50);
    
    autoReplySimulation();
}

function sendAudioMessage() {
    playSound('send');
    welcomeScreen.style.display = 'none';
    messagesContainer.style.display = 'flex';

    const msgId = 'msg-' + Date.now();

    let replyBlock = '';
    if (isReplying && replyMessageData) {
        replyBlock = `
            <div style="background: rgba(0,0,0,0.1); border-left: 3px solid #ffffff; padding: 4px 8px; border-radius: 4px; margin-bottom: 6px; font-size: 12px; opacity: 0.9; width: 100%;">
                <div style="font-weight: 600; margin-bottom: 2px;">${replyMessageData.isThem ? 'Sarah Jenkins' : 'You'}</div>
                <div style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 140px;">${replyMessageData.text}</div>
            </div>
        `;
        cancelReply();
    }

    const msgHTML = `
        <div class="msg-bubble-row me fade-in" id="${msgId}" data-type="audio" style="position: relative;">
            <input type="checkbox" class="msg-checkbox accent-blue-500" onchange="toggleSelect('${msgId}')">
            <div class="msg-bubble" style="padding: 6px 12px; display: flex; flex-direction: column;">
                ${replyBlock}
                <div class="msg-audio-box" style="min-width: 140px; padding: 0; display: flex; align-items: center; justify-content: space-between; gap: 8px;">
                    <button class="msg-audio-play-btn" style="flex-shrink: 0;">▶</button>
                    <div class="msg-audio-waves" style="flex: 1; justify-content: space-between; margin: 0;">
                        <div class="msg-wave-bar" style="height: 20%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 60%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 90%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 45%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 80%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 100%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 70%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 50%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 85%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 40%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 75%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 90%; opacity: 0.75"></div>
                        <div class="msg-wave-bar" style="height: 30%; opacity: 0.75"></div>
                    </div>
                    <div style="display: flex; flex-direction: column; align-items: flex-end; justify-content: center; min-width: 35px;">
                        <span class="msg-audio-time" style="margin: 0; line-height: 1;">0:0${Math.min(recordSeconds, 9)}</span>
                        <div style="display: flex; align-items: center; gap: 4px; margin-top: 4px;">
                            <span style="font-size: 9px; color: rgba(255,255,255,0.7);">${new Date().toLocaleTimeString('en-US', {hour: 'numeric', minute: '2-digit'})}</span>
                            <span class="msg-status-receipt" style="display:flex; align-items:center; height:12px;"></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    messagesContainer.insertAdjacentHTML('beforeend', msgHTML);
    setupLongPress(document.getElementById(msgId));
    updateMessageStatus(msgId);
    
    setTimeout(() => {
        chatArea.scrollTo({ top: chatArea.scrollHeight, behavior: 'smooth' });
    }, 50);
    
    autoReplySimulation();
}

let activeContextMenuMsg = null;

function setupLongPress(msgEl) {
    setupSwipeToReply(msgEl);
    let pressTimer1s;
    let pressTimer2s;
    let isPressed = false;
    
    msgEl.addEventListener('pointerdown', (e) => {
        if (isSelectionMode) return;
        if (e.target.tagName.toLowerCase() === 'input') return;
        
        isPressed = true;
        
        pressTimer1s = setTimeout(() => {
            if (!isPressed) return;
            showReactionsMenu(msgEl, e.clientX, e.clientY);
            
            pressTimer2s = setTimeout(() => {
                if (!isPressed) return;
                showFullActionMenu(msgEl, e.clientX, e.clientY);
            }, 1000);
            
        }, 1000);
    });

    const cancelPress = () => {
        isPressed = false;
        clearTimeout(pressTimer1s);
        clearTimeout(pressTimer2s);
    };

    msgEl.addEventListener('pointermove', (e) => {
        if (isPressed && (Math.abs(e.movementX) > 5 || Math.abs(e.movementY) > 5)) {
            cancelPress();
        }
    });

    msgEl.addEventListener('pointerup', cancelPress);
    msgEl.addEventListener('pointerleave', cancelPress);
    msgEl.addEventListener('pointercancel', cancelPress);
}

function showReactionsMenu(msgEl, x, y) {
    activeContextMenuMsg = msgEl;
    navigator.vibrate && navigator.vibrate(50);
    
    contextOverlay.innerHTML = `
        <div class="context-menu-container fade-in" style="top: ${Math.max(20, y - 60)}px; right: 20px;">
            <div class="reaction-bar">
                <span class="reaction-emoji" onclick="addReaction('❤️')">❤️</span>
                <span class="reaction-emoji" onclick="addReaction('👍')">👍</span>
                <span class="reaction-emoji" onclick="addReaction('👎')">👎</span>
                <span class="reaction-emoji" onclick="addReaction('😂')">😂</span>
                <span class="reaction-emoji" onclick="addReaction('‼️')">‼️</span>
                <span class="reaction-emoji" onclick="addReaction('❓')">❓</span>
            </div>
        </div>
    `;
    contextOverlay.classList.remove('opacity-0', 'pointer-events-none');
}

function showFullActionMenu(msgEl, x, y) {
    navigator.vibrate && navigator.vibrate(50);
    
    contextOverlay.innerHTML = `
        <div class="context-menu-container fade-in" style="top: ${Math.max(20, y - 60)}px; right: 20px;">
            <div class="reaction-bar mb-2">
                <span class="reaction-emoji" onclick="addReaction('❤️')">❤️</span>
                <span class="reaction-emoji" onclick="addReaction('👍')">👍</span>
                <span class="reaction-emoji" onclick="addReaction('👎')">👎</span>
                <span class="reaction-emoji" onclick="addReaction('😂')">😂</span>
                <span class="reaction-emoji" onclick="addReaction('‼️')">‼️</span>
                <span class="reaction-emoji" onclick="addReaction('❓')">❓</span>
            </div>
            <div class="action-menu">
                <div class="action-item" onclick="replyMessage()"><span>Reply</span> <i class="fas fa-reply text-gray-400"></i></div>
                <div class="action-item" onclick="copyMessage()"><span>Copy</span> <i class="far fa-copy text-gray-400"></i></div>
                <div class="action-item" onclick="enterSelectionMode()"><span>More...</span> <i class="fas fa-ellipsis-h text-gray-400"></i></div>
                <div class="action-item" onclick="editMessage()"><span>Edit</span> <i class="fas fa-pen text-gray-400"></i></div>
                <div class="action-item action-danger" onclick="unsendMessage()"><span>Undo Send</span> <i class="fas fa-trash"></i></div>
            </div>
        </div>
    `;
}

contextOverlay.addEventListener('click', (e) => {
    if (e.target === contextOverlay) closeContextMenu();
});

function closeContextMenu() {
    contextOverlay.classList.add('opacity-0', 'pointer-events-none');
    setTimeout(() => { contextOverlay.innerHTML = ''; activeContextMenuMsg = null; }, 200);
}

window.addReaction = function(emoji) {
    if (activeContextMenuMsg) {
        let bubble = activeContextMenuMsg.querySelector('.msg-bubble');
        let reactionsContainer = bubble.querySelector('.msg-reactions-badge');
        if (!reactionsContainer) {
            reactionsContainer = document.createElement('div');
            reactionsContainer.className = 'msg-reactions-badge';
            bubble.appendChild(reactionsContainer);
        }
        reactionsContainer.innerHTML = `<span>${emoji}</span>`;
    }
    closeContextMenu();
};

window.copyMessage = function() {
    if (activeContextMenuMsg && activeContextMenuMsg.dataset.text) {
        navigator.clipboard.writeText(activeContextMenuMsg.dataset.text);
    }
    closeContextMenu();
};

window.unsendMessage = function() {
    if (activeContextMenuMsg) {
        activeContextMenuMsg.style.transform = 'scale(0)';
        setTimeout(() => activeContextMenuMsg.remove(), 200);
    }
    closeContextMenu();
};

window.editMessage = function() {
    if (activeContextMenuMsg && activeContextMenuMsg.dataset.text) {
        messageInput.innerText = activeContextMenuMsg.dataset.text;
        messageInput.dispatchEvent(new Event('input'));
        activeContextMenuMsg.remove();
    }
    closeContextMenu();
};

let isReplying = false;
let replyMessageData = null;

window.replyMessage = function() {
    closeContextMenu();
    if (activeContextMenuMsg) {
        isReplying = true;
        const text = activeContextMenuMsg.dataset.text || 'Audio Message';
        replyMessageData = { text: text, isThem: activeContextMenuMsg.classList.contains('them') };
        
        document.getElementById('replyName').innerText = replyMessageData.isThem ? 'Sarah Jenkins' : 'You';
        document.getElementById('replyText').innerText = text;
        
        const rc = document.getElementById('replyContainer');
        rc.classList.remove('hidden');
        rc.classList.add('flex');
    }
    messageInput.focus();
};

window.enterSelectionMode = function() {
    isSelectionMode = true;
    selectedMessages.clear();
    
    if (activeContextMenuMsg) {
        const checkbox = activeContextMenuMsg.querySelector('.msg-checkbox');
        if (checkbox) {
            checkbox.checked = true;
            selectedMessages.add(activeContextMenuMsg.id);
        }
    }
    
    closeContextMenu();
    
    messagesContainer.classList.add('selection-mode');
    selectionHeader.classList.remove('hidden', '-translate-y-full');
    updateSelectionCount();
};

window.cancelSelection = function() {
    isSelectionMode = false;
    selectedMessages.clear();
    messagesContainer.classList.remove('selection-mode');
    selectionHeader.classList.add('-translate-y-full');
    setTimeout(() => selectionHeader.classList.add('hidden'), 300);
    
    document.querySelectorAll('.msg-checkbox').forEach(cb => cb.checked = false);
};

window.toggleSelect = function(id) {
    const cb = document.getElementById(id).querySelector('.msg-checkbox');
    if (cb.checked) {
        if (selectedMessages.size >= 5) {
            alert("Maximum 5 messages can be forwarded at once.");
            cb.checked = false;
            return;
        }
        selectedMessages.add(id);
    } else {
        selectedMessages.delete(id);
    }
    updateSelectionCount();
};

function updateSelectionCount() {
    document.getElementById('selectionCount').innerText = selectedMessages.size;
}

window.forwardSelected = function() {
    if (selectedMessages.size === 0) return;
    alert(`Forwarding ${selectedMessages.size} messages to up to 10 users!`);
    cancelSelection();
};

function updateMessageStatus(msgId) {
    const msgRow = document.getElementById(msgId);
    if (!msgRow) return;
    const statusSpan = msgRow.querySelector('.msg-status-receipt');
    
    statusSpan.innerHTML = '<i data-lucide="check" stroke-width="1.5" style="width:16px;height:16px;color:#ffffff;"></i>';
    if(window.lucide) lucide.createIcons({root: statusSpan});

    setTimeout(() => {
        statusSpan.innerHTML = '<i data-lucide="check-check" stroke-width="1.5" style="width:16px;height:16px;color:#ffffff;"></i>';
        if(window.lucide) lucide.createIcons({root: statusSpan});
        
        setTimeout(() => {
            statusSpan.innerHTML = '<i data-lucide="check-check" stroke-width="1.5" style="width:16px;height:16px;color:#4ade80;"></i>';
            if(window.lucide) lucide.createIcons({root: statusSpan});
        }, 2000);
    }, 1000);
}


let isSimulating = false;

function autoReplySimulation() {
    if (isSimulating) return;
    isSimulating = true;

    const headerStatus = document.getElementById('headerStatus');
    
    // 1. Typing State (after 1s)
    setTimeout(() => {
        headerStatus.innerHTML = '<span class="text-blue-500">typing...</span>';
        
        const typingId = 'typing-' + Date.now();
        const typingHTML = `
            <div class="msg-bubble-row them fade-in" id="${typingId}">
                <div class="msg-bubble" style="padding: 12px 16px;">
                    <div class="msg-typing-indicator">
                        <div class="msg-typing-dot"></div>
                        <div class="msg-typing-dot"></div>
                        <div class="msg-typing-dot"></div>
                    </div>
                </div>
            </div>
        `;
        messagesContainer.insertAdjacentHTML('beforeend', typingHTML);
        chatArea.scrollTo({ top: chatArea.scrollHeight, behavior: 'smooth' });

        // 2. Recording State (after 3s)
        setTimeout(() => {
            document.getElementById(typingId).remove();
            headerStatus.innerHTML = '<span class="text-red-500 flex items-center gap-1"><i data-lucide="mic" class="w-3 h-3"></i> recording audio...</span>';
            if(window.lucide) lucide.createIcons({root: headerStatus});

            // 3. Send Audio (after 4s)
            setTimeout(() => {
                headerStatus.innerHTML = '<span class="text-blue-500">Online</span>';
                
                playSound('receive');
                const audioMsgId = 'sim-' + Date.now();
                const audioHTML = `
                    <div class="msg-bubble-row them fade-in" id="${audioMsgId}" data-type="audio" data-text="Audio Message">
                        <div class="msg-bubble" style="padding: 6px 12px;">
                            <div class="msg-audio-box" style="min-width: 140px; padding: 0; display: flex; align-items: center; justify-content: space-between; gap: 8px;">
                                <button class="msg-audio-play-btn" style="flex-shrink: 0;">▶</button>
                                <div class="msg-audio-waves" style="flex: 1; justify-content: space-between; margin: 0;">
                                    <div class="msg-wave-bar" style="height: 20%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 60%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 90%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 45%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 80%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 100%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 70%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 50%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 85%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 40%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 75%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 90%; opacity: 0.75"></div>
                                    <div class="msg-wave-bar" style="height: 30%; opacity: 0.75"></div>
                                </div>
                                <div style="display: flex; flex-direction: column; align-items: flex-end; justify-content: center; min-width: 35px;">
                                    <span class="msg-audio-time" style="margin: 0; line-height: 1;">0:04</span>
                                    <span style="font-size: 9px; color: #8e8e93; margin-top: 4px;">${new Date().toLocaleTimeString('en-US', {hour: 'numeric', minute: '2-digit'})}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                messagesContainer.insertAdjacentHTML('beforeend', audioHTML);
                setupLongPress(document.getElementById(audioMsgId));
                chatArea.scrollTo({ top: chatArea.scrollHeight, behavior: 'smooth' });

                // 4. Back to Normal (after 3s)
                setTimeout(() => {
                    headerStatus.innerHTML = 'Verified Business';
                    isSimulating = false;
                }, 3000);
            }, 4000);
        }, 3000);
    }, 1500);
}


function setupSwipeToReply(msgEl) {
    const bubble = msgEl.querySelector('.msg-bubble');
    if (!bubble) return;
    let startX = 0;
    let currentX = 0;
    let isSwiping = false;
    let startY = 0;

    bubble.addEventListener('touchstart', (e) => {
        if (isSelectionMode) return;
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
        isSwiping = true;
        bubble.style.transition = 'none';
    }, {passive: true});

    bubble.addEventListener('touchmove', (e) => {
        if (!isSwiping) return;
        currentX = e.touches[0].clientX;
        let currentY = e.touches[0].clientY;
        
        if (Math.abs(currentY - startY) > Math.abs(currentX - startX)) {
            // vertical scrolling, cancel swipe
            isSwiping = false;
            bubble.style.transition = 'transform 0.3s cubic-bezier(0.25, 1.5, 0.5, 1)';
            bubble.style.transform = '';
            return;
        }

        let deltaX = currentX - startX;
        if (Math.abs(deltaX) > 60) {
            deltaX = (deltaX > 0 ? 60 : -60) + (deltaX - (deltaX > 0 ? 60 : -60)) * 0.2;
        }
        bubble.style.transform = `translateX(${deltaX}px)`;
    }, {passive: true});

    bubble.addEventListener('touchend', (e) => {
        if (!isSwiping) return;
        isSwiping = false;
        bubble.style.transition = 'transform 0.3s cubic-bezier(0.25, 1.5, 0.5, 1)';
        bubble.style.transform = '';

        let deltaX = currentX - startX;
        if (Math.abs(deltaX) > 50) {
            navigator.vibrate && navigator.vibrate(50);
            activeContextMenuMsg = msgEl;
            replyMessage();
        }
    });
}


window.cancelReply = function() {
    document.getElementById('replyContainer').classList.replace('flex', 'hidden');
    isReplying = false;
    replyMessageData = null;
};

