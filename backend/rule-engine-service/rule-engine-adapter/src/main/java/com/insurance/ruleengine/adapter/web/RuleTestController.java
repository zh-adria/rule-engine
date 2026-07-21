package com.insurance.ruleengine.adapter.web;

import com.insurance.ruleengine.client.api.RuleTestFacade;
import com.insurance.ruleengine.client.dto.RuleTestCaseDTO;
import com.insurance.ruleengine.client.dto.RuleTestRunDTO;
import com.insurance.ruleengine.client.dto.RuleTestSuiteDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rule-tests")
public class RuleTestController {
    private final RuleTestFacade ruleTestFacade;

    public RuleTestController(RuleTestFacade ruleTestFacade) {
        this.ruleTestFacade = ruleTestFacade;
    }

    @Operation(summary = "查询规则测试用例")
    @GetMapping("/cases")
    public List<RuleTestCaseDTO> listCases(@RequestParam(required = false) String ruleCode,
                                           @RequestParam(required = false) Boolean enabled) {
        return ruleTestFacade.listCases(ruleCode, enabled);
    }

    @Operation(summary = "创建规则测试用例")
    @PostMapping("/cases")
    public ResponseEntity<RuleTestCaseDTO> createCase(@RequestBody RuleTestCaseDTO testCase) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleTestFacade.createCase(testCase));
    }

    @Operation(summary = "查询单个规则测试用例")
    @GetMapping("/cases/{caseCode}")
    public ResponseEntity<RuleTestCaseDTO> getCase(@PathVariable String caseCode) {
        return ruleTestFacade.findCase(caseCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新规则测试用例")
    @PutMapping("/cases/{caseCode}")
    public RuleTestCaseDTO updateCase(@PathVariable String caseCode, @RequestBody RuleTestCaseDTO testCase) {
        return ruleTestFacade.updateCase(caseCode, testCase);
    }

    @Operation(summary = "删除规则测试用例")
    @DeleteMapping("/cases/{caseCode}")
    public ResponseEntity<Void> deleteCase(@PathVariable String caseCode) {
        ruleTestFacade.deleteCase(caseCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "运行单个规则测试用例")
    @PostMapping("/cases/{caseCode}/run")
    public RuleTestRunDTO runCase(@PathVariable String caseCode,
                                  @RequestParam(required = false, defaultValue = "tester") String executedBy) {
        return ruleTestFacade.runCase(caseCode, executedBy);
    }

    @Operation(summary = "查询规则测试套件")
    @GetMapping("/suites")
    public List<RuleTestSuiteDTO> listSuites(@RequestParam(required = false) String ruleCode,
                                             @RequestParam(required = false) String businessLine,
                                             @RequestParam(required = false) Boolean enabled) {
        return ruleTestFacade.listSuites(ruleCode, businessLine, enabled);
    }

    @Operation(summary = "创建规则测试套件")
    @PostMapping("/suites")
    public ResponseEntity<RuleTestSuiteDTO> createSuite(@RequestBody RuleTestSuiteDTO suite) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleTestFacade.createSuite(suite));
    }

    @Operation(summary = "查询单个规则测试套件")
    @GetMapping("/suites/{suiteCode}")
    public ResponseEntity<RuleTestSuiteDTO> getSuite(@PathVariable String suiteCode) {
        return ruleTestFacade.findSuite(suiteCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "更新规则测试套件")
    @PutMapping("/suites/{suiteCode}")
    public RuleTestSuiteDTO updateSuite(@PathVariable String suiteCode, @RequestBody RuleTestSuiteDTO suite) {
        return ruleTestFacade.updateSuite(suiteCode, suite);
    }

    @Operation(summary = "删除规则测试套件")
    @DeleteMapping("/suites/{suiteCode}")
    public ResponseEntity<Void> deleteSuite(@PathVariable String suiteCode) {
        ruleTestFacade.deleteSuite(suiteCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "添加测试用例到套件")
    @PostMapping("/suites/{suiteCode}/cases")
    public ResponseEntity<Void> addCaseToSuite(@PathVariable String suiteCode, @RequestBody SuiteCaseRequest request) {
        ruleTestFacade.addCaseToSuite(suiteCode, request.getCaseCode(), request.getCaseOrder());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "从套件移除测试用例")
    @DeleteMapping("/suites/{suiteCode}/cases/{caseCode}")
    public ResponseEntity<Void> removeCaseFromSuite(@PathVariable String suiteCode, @PathVariable String caseCode) {
        ruleTestFacade.removeCaseFromSuite(suiteCode, caseCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "运行规则测试套件")
    @PostMapping("/suites/{suiteCode}/run")
    public RuleTestRunDTO runSuite(@PathVariable String suiteCode,
                                   @RequestParam(required = false, defaultValue = "tester") String executedBy) {
        return ruleTestFacade.runSuite(suiteCode, executedBy);
    }

    @Operation(summary = "查询规则测试运行记录")
    @GetMapping("/runs")
    public List<RuleTestRunDTO> listRuns(@RequestParam(required = false) String ruleCode,
                                         @RequestParam(required = false) String suiteCode,
                                         @RequestParam(required = false) String caseCode) {
        return ruleTestFacade.listRuns(ruleCode, suiteCode, caseCode);
    }

    @Operation(summary = "查询单个规则测试运行记录")
    @GetMapping("/runs/{runId}")
    public ResponseEntity<RuleTestRunDTO> getRun(@PathVariable String runId) {
        return ruleTestFacade.findRun(runId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public static class SuiteCaseRequest {
        private String caseCode;
        private Integer caseOrder;

        public String getCaseCode() { return caseCode; }
        public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
        public Integer getCaseOrder() { return caseOrder; }
        public void setCaseOrder(Integer caseOrder) { this.caseOrder = caseOrder; }
    }
}
