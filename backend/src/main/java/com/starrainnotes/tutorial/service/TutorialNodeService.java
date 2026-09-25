package com.starrainnotes.tutorial.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.tutorial.dto.MoveIndexRequest;
import com.starrainnotes.tutorial.dto.MoveNodeRequest;
import com.starrainnotes.tutorial.entity.TutorialNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compatibility adapter for the old generic move route. It may only sort
 * root groups or chapters inside their current group.
 */
@Service
@RequiredArgsConstructor
public class TutorialNodeService {
    private final TutorialNodeAccess access;
    private final TutorialGroupCommandService groups;
    private final TutorialChapterCommandService chapters;

    @Transactional
    public void moveNode(Long tutorialId, Long nodeId, MoveNodeRequest request) {
        TutorialNode node = access.requireNode(tutorialId, nodeId);
        if (TutorialNodeAccess.GROUP.equals(node.getNodeType())) {
            if (request.targetParentId() != null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_NESTING_FORBIDDEN",
                        "Groups cannot be nested", "Curriculum groups are root-level siblings.");
            }
            groups.placeGroup(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));
            return;
        }
        if (!java.util.Objects.equals(node.getParentId(), request.targetParentId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CROSS_GROUP_DRAG_FORBIDDEN",
                    "Cross-group drag is not allowed",
                    "Use the explicit chapter reassignment action to change a chapter's group.");
        }
        chapters.placeChapter(tutorialId, nodeId, new MoveIndexRequest(request.targetIndex()));
    }
}
