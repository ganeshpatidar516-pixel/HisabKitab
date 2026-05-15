package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.adapters.hisabkitab.SafeNoOpHisabKitabAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard

object SuperCommandFactory {
    fun createSafeDefault(): SuperCommandOrchestrator {
        return SuperCommandOrchestrator(
            normalizer = InputNormalizer(),
            dialectRegistry = DialectRegistry(),
            parser = DeterministicIntentParser(),
            policyGuard = PolicyGuard(),
            adapter = SafeNoOpHisabKitabAdapter()
        )
    }
}
