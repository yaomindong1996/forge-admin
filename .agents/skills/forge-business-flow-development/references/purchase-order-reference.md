# Purchase Order Reference

Use the current purchase order approval as the canonical code-first example.

## Main Files

- Entity: `forge-server/forge-business/forge-business-core/src/main/java/com/mdframe/forge/business/core/purchase/domain/SamplePurchaseOrder.java`
- Controller: `.../purchase/controller/SamplePurchaseOrderController.java`
- Service: `.../purchase/service/impl/SamplePurchaseOrderServiceImpl.java`
- Flow constants: `.../purchase/support/SamplePurchaseOrderFlowDefinition.java`
- BPMN builder: `.../purchase/support/SamplePurchaseOrderFlowBpmn.java`
- Provider: `.../purchase/provider/SamplePurchaseOrderCodeFormProvider.java`
- Mapper XML: `forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/SamplePurchaseOrderMapper.xml`
- Frontend API: `forge-admin-ui/src/api/business/purchase-order-test.js`
- Frontend page/form: `forge-admin-ui/src/views/business/purchase-order-test.vue`
- Table/dict/menu seed: `forge-server/db/mysql/migration/V1.0.81__add_sample_purchase_order_flow_test.sql`
- Flow binding seed: `forge-server/db/mysql/migration/V1.0.82__seed_sample_purchase_order_flow_binding.sql`
- App Center seed: `forge-server/db/mysql/migration/V1.0.83__seed_sample_purchase_order_app_center_entry.sql`
- Fallback node permission patch: `forge-server/db/mysql/migration/V1.0.84__extend_sample_purchase_order_form_asset_fields.sql`

## Business Contract

- Object code/business type: `sample_purchase_order`
- Flow model key: `sample_purchase_order_approval`
- Provider key: `samplePurchaseOrder`
- Form key: `sample_purchase_order_approval_form`
- Form mode: `BUSINESS_CODE_FORM`
- Business key: `sample_purchase_order:{id}`

## Node Keys

- `dept_leader_approve`: department leader approval.
- `engineering_manager_approve`: engineering manager approval.
- `purchase_countersign`: parallel multi-instance countersign.
- `applicant_modify`: applicant modifies and resubmits or terminates.

## Variables

Start variables include:

- `businessKey`
- `objectCode`
- `recordId`
- `purchaseOrderId`
- `orderNo`
- `title`
- `amountCent`
- `initiator`
- `deptLeaderId`
- `engineeringManagerId`
- `countersignUserList`
- `ccRoleKeys`

Task completion variables used by gateways/status:

- `approvalResult`: `approve` or `reject`
- `approved`: boolean; countersign completion uses false to stop early on rejection

## Status Behavior

- New record: `DRAFT`.
- Submit: start Flowable, write `business_key/process_instance_id`, set `IN_PROCESS`.
- Reject from approval/countersign: set `NEED_MODIFY`.
- Applicant modify approve/resubmit: set `IN_PROCESS`.
- Applicant modify reject/terminate: set `REJECTED`.
- Process completed: set `APPROVED`.
- Process canceled: set `CANCELED`.

The sample also repairs drift:

- `TASK_CREATED` for `applicant_modify` repairs `IN_PROCESS -> NEED_MODIFY`.
- `TASK_CREATED` for approval nodes repairs `NEED_MODIFY -> IN_PROCESS`.
- Task save methods repair the same transitions before validating.
- Page/detail queries reconcile running status by reading active `sys_flow_task.task_def_key`.

## Form Integration

`SamplePurchaseOrderCodeFormProvider` registers a code form asset with:

- `formKey`
- `formName`
- `formMode=BUSINESS_CODE_FORM`
- `providerKey`
- `formUrl`
- `fields` / `fieldCatalog`
- `supportsSave=true`

`buildContext` loads business record detail and returns `BusinessTaskFormContextVO`. `saveContext` converts platform payload into `SamplePurchaseOrderTaskSaveDTO`, then calls `saveTaskFields`.

## BPMN Pattern

The BPMN user tasks set:

- `flowable:assignee="${deptLeaderId}"` style variable expressions.
- `flowable:formKey="sample_purchase_order_approval_form"`.
- `flowable:formFieldPermissions='[...]'`.
- Approval button flags such as `flowable:allowApprove`, `flowable:allowReject`, `flowable:allowDelegate`, `flowable:requireComment`.

Gateways use reject conditions:

```xml
<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
```

Countersign uses:

```xml
<bpmn:multiInstanceLoopCharacteristics isSequential="false"
    flowable:collection="${countersignUserList}"
    flowable:elementVariable="assignee">
  <bpmn:completionCondition xsi:type="bpmn:tFormalExpression"><![CDATA[${approved == false || nrOfCompletedInstances == nrOfInstances}]]></bpmn:completionCondition>
</bpmn:multiInstanceLoopCharacteristics>
```

## Initialization Rule

`ensureFlowModel()` may create a default model when absent or when existing BPMN XML is empty. It must not overwrite existing non-empty BPMN XML, because the flow designer is the owner of node form permissions and approval settings after users save the model.
