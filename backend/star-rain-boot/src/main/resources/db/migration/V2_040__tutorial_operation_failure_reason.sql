-- Additive upgrade: retain all existing proposals, versions and review decisions.
ALTER TABLE sr_tutorial_operation ADD COLUMN execution_message VARCHAR(500) NULL;
