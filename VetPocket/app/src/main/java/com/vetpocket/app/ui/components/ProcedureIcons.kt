package com.vetpocket.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.ui.graphics.vector.ImageVector
import com.vetpocket.app.data.db.ProcedureType

fun ProcedureType.icon(): ImageVector = when (this) {
    ProcedureType.COMPLEX_VACCINE, ProcedureType.RABIES -> Icons.Rounded.Vaccines
    ProcedureType.ANTI_TICK -> Icons.Rounded.BugReport
    ProcedureType.ANTI_WORM -> Icons.Rounded.Medication
}
