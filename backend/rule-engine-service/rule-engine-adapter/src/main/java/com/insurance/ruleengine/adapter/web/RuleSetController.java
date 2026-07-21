package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleEngineFacade;
import com.insurance.ruleengine.client.dto.CreateRuleSetCmd;
import com.insurance.ruleengine.client.dto.ExecuteRuleSetCmd;
import com.insurance.ruleengine.client.dto.RuleSetDTO;
import com.insurance.ruleengine.client.dto.RuleSetExecutionResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/rule-sets")
public class RuleSetController {
    private final RuleEngineFacade ruleEngineFacade;

    public RuleSetController(RuleEngineFacade ruleEngineFacade) {
        this.ruleEngineFacade = ruleEngineFacade;
    }

    @Operation(summary = "创建规则集", description = "创建规则编排集")
    @PostMapping
    public RuleSetDTO createRuleSet(@Valid @RequestBody CreateRuleSetCmd cmd) {
        return ruleEngineFacade.createRuleSet(cmd);
    }

    @Operation(summary = "查询规则集")
    @GetMapping("/{setCode}")
    public RuleSetDTO getRuleSet(@PathVariable String setCode) {
        return ruleEngineFacade.getRuleSet(setCode);
    }

    @Operation(summary = "查询规则集列表")
    @GetMapping
    public List<RuleSetDTO> listRuleSets() {
        return ruleEngineFacade.listRuleSets();
    }

    @Operation(summary = "更新规则集")
    @PutMapping("/{setCode}")
    public RuleSetDTO updateRuleSet(@PathVariable String setCode, @Valid @RequestBody CreateRuleSetCmd cmd) {
        return ruleEngineFacade.updateRuleSet(setCode, cmd);
    }

    @Operation(summary = "删除规则集")
    @DeleteMapping("/{setCode}")
    public void deleteRuleSet(@PathVariable String setCode) {
        ruleEngineFacade.deleteRuleSet(setCode);
    }

    @Operation(summary = "执行规则集", description = "按 SERIAL/PARALLEL 编排执行规则集")
    @PostMapping("/execute")
    public RuleSetExecutionResultDTO executeRuleSet(@Valid @RequestBody ExecuteRuleSetCmd cmd) {
        return ruleEngineFacade.executeRuleSet(cmd);
    }
}
