package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mandatory storage setup, shown after the welcome narration whenever no task
 * file has been chosen yet. Entropy's Defiance keeps every task in a plain JSON
 * file the user selects — this is what makes Syncthing sync work — so the app
 * cannot persist anything until a location exists. Gating here (rather than
 * silently using an internal default) keeps the sync-first model explicit and
 * prevents the "created tasks vanish" trap of an unconfigured file path.
 */
@Composable
fun StorageSetupScreen(
    primaryColor: Color,
    onChooseFolder: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Choose where tasks live",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 42.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W100
                )

                Spacer(height = 28)

                Text(
                    text = "Your tasks are stored in a single plain-text JSON " +
                        "file that you own. Pick a folder — put it in a Syncthing " +
                        "directory to keep every device in step. A tasks.json file " +
                        "is created there for you.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 28.sp
                    ),
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W400
                )

                Spacer(height = 64)

                Button(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 200.dp)
                ) {
                    Text(
                        text = "Choose folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W500,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Spacer(height: Int) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height.dp))
}
