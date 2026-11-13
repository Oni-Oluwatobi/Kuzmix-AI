package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.DomainBridge
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBridgeTest {

    @Test
    fun legacyExactMatchStillWorks() {
        assertTrue(DomainBridge.matches("subject.physics", "subject.physics"))
        assertTrue(DomainBridge.matches("sci_math", "sci_math"))
    }

    @Test
    fun subjectXEqualsDomainX() {
        assertTrue(DomainBridge.matches("subject.sci_math", "sci_math"))
    }

    @Test
    fun scienceSubjectsBridgeToSciMath() {
        assertTrue(DomainBridge.matches("subject.physics", "sci_math"))
        assertTrue(DomainBridge.matches("subject.chemistry", "sci_math"))
        assertTrue(DomainBridge.matches("subject.mathematics", "sci_math"))
    }

    @Test
    fun biologyBridgesToHealthAndSciMath() {
        assertTrue(DomainBridge.matches("subject.biology", "health_life"))
        assertTrue(DomainBridge.matches("subject.biology", "sci_math"))
        assertFalse(DomainBridge.matches("subject.biology", "business_econ"))
    }

    @Test
    fun economicsAndGovernmentBridge() {
        assertTrue(DomainBridge.matches("subject.economics", "business_econ"))
        assertTrue(DomainBridge.matches("subject.government", "society_culture"))
        assertFalse(DomainBridge.matches("subject.economics", "creative_arts"))
    }

    @Test
    fun computerScienceBridgesToComputingAndEngineering() {
        assertTrue(DomainBridge.matches("subject.computer_science", "computing_ai"))
        assertTrue(DomainBridge.matches("subject.computer_science", "eng_tech"))
    }

    @Test
    fun generalKnowledgeBridgesToEveryDomain() {
        assertTrue(DomainBridge.matches("subject.general_knowledge", "sci_math"))
        assertTrue(DomainBridge.matches("subject.general_knowledge", "creative_arts"))
        assertTrue(DomainBridge.matches("subject.general_knowledge", "practical_life"))
    }

    @Test
    fun unrelatedSubjectDomainPairDoesNotMatch() {
        assertFalse(DomainBridge.matches("subject.crs", "computing_ai"))
        assertFalse(DomainBridge.matches("subject.literature", "sci_math"))
        assertFalse(DomainBridge.matches("subject.english_language", "business_econ"))
    }

    @Test
    fun unknownSubjectNeverMatches() {
        assertFalse(DomainBridge.matches("subject.unknown_thing", "sci_math"))
        assertFalse(DomainBridge.matches("random_value", "sci_math"))
    }
}
