-- CHECK constraints describe the root/group shape, while these guards verify
-- the referenced parent row is an actual root GROUP. The existing composite
-- foreign key already guarantees that parent and child share a tutorial.
DELIMITER //

CREATE TRIGGER trg_tutorial_node_curriculum_insert
BEFORE INSERT ON tutorial_node
FOR EACH ROW
BEGIN
    IF NEW.node_type = 'CHAPTER' AND NOT EXISTS (
        SELECT 1
        FROM tutorial_node parent
        WHERE parent.id = NEW.parent_id
          AND parent.tutorial_id = NEW.tutorial_id
          AND parent.node_type = 'GROUP'
          AND parent.parent_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'CHAPTER parent must be a root GROUP in the same tutorial';
    END IF;
END//

CREATE TRIGGER trg_tutorial_node_curriculum_update
BEFORE UPDATE ON tutorial_node
FOR EACH ROW
BEGIN
    IF NEW.node_type = 'CHAPTER' AND NOT EXISTS (
        SELECT 1
        FROM tutorial_node parent
        WHERE parent.id = NEW.parent_id
          AND parent.tutorial_id = NEW.tutorial_id
          AND parent.node_type = 'GROUP'
          AND parent.parent_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'CHAPTER parent must be a root GROUP in the same tutorial';
    END IF;
END//

DELIMITER ;
