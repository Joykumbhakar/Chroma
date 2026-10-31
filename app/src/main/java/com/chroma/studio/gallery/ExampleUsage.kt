// Example: show the gallery from an Activity (full-screen) or as a bottom sheet.
//
// class MainActivity : ComponentActivity() {
//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)
//         enableEdgeToEdge()
//         setContent {
//             var showGallery by remember { mutableStateOf(true) }
//             if (showGallery) {
//                 OscellionGallery(
//                     mode = GalleryLayoutMode.BottomSheet,      // or FullScreen
//                     maxSelection = 10,
//                     showCaptionInput = true,
//                     onConfirm = { items, caption ->
//                         // items: List<MediaItem> â€“ use item.uri (content://) to upload/send
//                         showGallery = false
//                     },
//                     onClose = { showGallery = false },
//                     onSystemPicked = { uris -> /* URIs from the system photo picker */ },
//                 )
//             }
//         }
//     }
// }
