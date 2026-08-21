package fr.mandarine.todolist.ui.paper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier

class PaperGalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaperTheme {
                PaperSurface {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        RuledRowPreview()
                        GhostRowPreview()
                        CountBadgePreview()
                        InkIconPreview()
                        StickyNotePadPreview()
                    }
                }
            }
        }
    }
}
