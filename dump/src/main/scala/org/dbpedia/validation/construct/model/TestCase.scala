package org.dbpedia.validation.construct.model

case class TestCase(ID: TestCaseID, triggerID: TriggerID, validatorID: ValidatorID, TYPE: TestCaseType.Value)