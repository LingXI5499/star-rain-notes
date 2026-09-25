package com.starrainnotes.tutorial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.tutorial.assembler.TutorialNodeAssembler;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.CreateGroupRequest;
import com.starrainnotes.tutorial.dto.MoveIndexRequest;
import com.starrainnotes.tutorial.dto.UpdateGroupRequest;
import com.starrainnotes.tutorial.entity.TutorialNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Group commands for the fixed two-level curriculum. */
@Service
@RequiredArgsConstructor
public class TutorialGroupCommandService {
    private final TutorialNodeAccess access;
    private final TutorialNodeAssembler nodeAssembler;

    @Transactional
    public AdminTreeNodeView createGroup(Long tutorialId, CreateGroupRequest request) {
        access.requireTutorial(tutorialId);
        TutorialNode group = new TutorialNode();
        group.setTutorialId(tutorialId);
        group.setParentId(null);
        group.setNodeType(TutorialNodeAccess.GROUP);
        group.setTitle(request.title());
        group.setSortOrder(access.nextGroupOrder(tutorialId));
        access.nodes().insert(group);
        return nodeAssembler.toTreeNode(group);
    }

    @Transactional
    public AdminTreeNodeView updateGroup(Long tutorialId, Long groupId, UpdateGroupRequest request) {
        TutorialNode group = access.requireGroup(tutorialId, groupId);
        group.setTitle(request.title());
        access.nodes().updateById(group);
        return nodeAssembler.toTreeNode(group);
    }

    @Transactional
    public void deleteGroup(Long tutorialId, Long groupId) {
        access.requireGroup(tutorialId, groupId);
        Long children = access.nodes().selectCount(new LambdaQueryWrapper<TutorialNode>()
                .eq(TutorialNode::getParentId, groupId));
        if (children != null && children > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GROUP_HAS_CHILDREN",
                    "Group has chapters", "A group containing chapters cannot be deleted.");
        }
        access.nodes().deleteById(groupId);
        access.normalizeOrders(access.loadGroups(tutorialId));
    }

    @Transactional
    public void moveGroup(Long tutorialId, Long groupId, MoveIndexRequest request) {
        placeGroup(tutorialId, groupId, request);
    }

    public void placeGroup(Long tutorialId, Long groupId, MoveIndexRequest request) {
        TutorialNode group = access.requireGroup(tutorialId, groupId);
        List<TutorialNode> groups = access.loadGroups(tutorialId);
        groups.removeIf(item -> item.getId().equals(groupId));
        groups.add(Math.min(Math.max(request.targetIndex(), 0), groups.size()), group);
        access.normalizeOrders(groups);
    }
}
