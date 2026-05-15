package com.ganesh.hisabkitabpro.domain.ai.control

import com.ganesh.hisabkitabpro.engine.AiActionResult
import com.ganesh.hisabkitabpro.engine.AiCommandRouter

class AIAppController(
    private val commandRouter: AiCommandRouter
) {
    /**
     * ब्लूप्रिंट के अनुसार Intent Detection और Entity Extraction करता है।
     */
    suspend fun executeCommand(command: String): AIAction {
        val result = commandRouter.routeCommand(command)
        
        return when (result) {
            is AiActionResult.NavigateToMarketing -> AIAction.NavigateToMarketing
            is AiActionResult.NavigateToLedger -> AIAction.NavigateToLedger(result.customerId)
            is AiActionResult.Success -> AIAction.ShowMessage(result.message)
            is AiActionResult.Error -> AIAction.ShowMessage("Error: ${result.message}")
        }
    }
}

sealed class AIAction {
    object NavigateToMarketing : AIAction()
    data class NavigateToLedger(val customerId: Long) : AIAction()
    data class ShowMessage(val message: String) : AIAction()
}
