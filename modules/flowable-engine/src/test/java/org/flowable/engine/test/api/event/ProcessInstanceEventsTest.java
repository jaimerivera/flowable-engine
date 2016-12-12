/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.api.event;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.delegate.event.impl.FlowableActivityCancelledEventImpl;
import org.flowable.engine.impl.delegate.event.ActivitiEngineEntityEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

/**
 * Test case for all {@link FlowableEvent}s related to process instances.
 * 
 * @author Tijs Rademakers
 */
public class ProcessInstanceEventsTest extends PluggableFlowableTestCase {

  private TestInitializedEntityEventListener listener;

  /**
   * Test create, update and delete events of process instances.
   */
  @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testProcessInstanceEvents() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    assertNotNull(processInstance);

    // Check create-event
    assertEquals(6, listener.getEventsReceived().size());
    assertTrue(listener.getEventsReceived().get(0) instanceof ActivitiEngineEntityEvent);

    // process instance create event
    ActivitiEngineEntityEvent event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());

    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
    assertEquals(FlowableEngineEventType.PROCESS_CREATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());

    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(2);
    assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());

    // start event create event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(3);
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());

    // start event create initialized
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(4);
    assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(5);
   assertEquals(FlowableEngineEventType.PROCESS_STARTED, event.getType());
   assertEquals(processInstance.getId(), event.getProcessInstanceId());
   assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
   assertTrue(event instanceof FlowableProcessStartedEvent);
   assertNull(((FlowableProcessStartedEvent)event).getNestedProcessDefinitionId());
   assertNull(((FlowableProcessStartedEvent)event).getNestedProcessInstanceId());
    
    listener.clearEventsReceived();

    // Check update event when suspended/activated
    runtimeService.suspendProcessInstanceById(processInstance.getId());
    runtimeService.activateProcessInstanceById(processInstance.getId());

    assertEquals(4, listener.getEventsReceived().size());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
    assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(2);
    assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(3);
    assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    listener.clearEventsReceived();

    // Check update event when process-definition is suspended (should
    // cascade suspend/activate all process instances)
    repositoryService.suspendProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);
    repositoryService.activateProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);

    assertEquals(4, listener.getEventsReceived().size());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
    assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(2);
    assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(3);
    assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    listener.clearEventsReceived();

    // Check update-event when business-key is updated
    runtimeService.updateBusinessKey(processInstance.getId(), "thekey");
    assertEquals(1, listener.getEventsReceived().size());
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    listener.clearEventsReceived();

    runtimeService.deleteProcessInstance(processInstance.getId(), "Testing events");

    List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals(1, processCancelledEvents.size());
    FlowableCancelledEvent cancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
    assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, cancelledEvent.getType());
    assertEquals(processInstance.getId(), cancelledEvent.getProcessInstanceId());
    assertEquals(processInstance.getId(), cancelledEvent.getExecutionId());
    listener.clearEventsReceived();
  }

  /**
   * Test create, update and delete events of process instances.
   */
  @Deployment(resources = { "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
  public void testSubProcessInstanceEvents() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    assertNotNull(processInstance);
    String processDefinitionId = processInstance.getProcessDefinitionId();

    // Check create-event one main process the second one Scope execution, and the third one subprocess
    assertEquals(11, listener.getEventsReceived().size());
    assertTrue(listener.getEventsReceived().get(0) instanceof ActivitiEngineEntityEvent);

    // process instance created event
    ActivitiEngineEntityEvent event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(0);
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    assertEquals(processInstance.getId(), ((ProcessInstance) event.getEntity()).getId());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), event.getExecutionId());
    assertEquals(processDefinitionId, event.getProcessDefinitionId());

    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(1);
    String processExecutionId = event.getExecutionId();
    assertEquals(FlowableEngineEventType.PROCESS_CREATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), processExecutionId);
    assertEquals(processDefinitionId, event.getProcessDefinitionId());

    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(2);
    processExecutionId = event.getExecutionId();
    assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getId(), processExecutionId);
    assertEquals(processDefinitionId, event.getProcessDefinitionId());

    // start event created event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(3);
    processExecutionId = event.getExecutionId();
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), processExecutionId);
    assertEquals(processDefinitionId, event.getProcessDefinitionId());

    // start event initialized event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(4);
    assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertNotEquals(processInstance.getId(), ((ExecutionEntity) event.getEntity()).getId());

    // Process start
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(5);
    assertEquals(FlowableEngineEventType.PROCESS_STARTED, event.getType());
    assertEquals(processInstance.getId(), event.getProcessInstanceId());
    assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
    assertTrue(event instanceof FlowableProcessStartedEvent);
    assertNull(((FlowableProcessStartedEvent)event).getNestedProcessDefinitionId());
    assertNull(((FlowableProcessStartedEvent)event).getNestedProcessInstanceId());

    // sub process instance created event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(6);
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    ExecutionEntity subProcessEntity = (ExecutionEntity) event.getEntity();
    assertEquals(processExecutionId, subProcessEntity.getSuperExecutionId());
    String subProcessInstanceId = subProcessEntity.getProcessInstanceId();

    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(7);
    assertEquals(FlowableEngineEventType.PROCESS_CREATED, event.getType());
    subProcessEntity = (ExecutionEntity) event.getEntity();
    assertEquals(processExecutionId, subProcessEntity.getSuperExecutionId());

    // sub process instance start created event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(8);
    assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
    assertEquals(subProcessInstanceId, event.getProcessInstanceId());
    assertNotEquals(subProcessInstanceId, event.getExecutionId());
    String subProcessDefinitionId = ((ExecutionEntity) event.getEntity()).getProcessDefinitionId();
    assertNotNull(subProcessDefinitionId);
    ProcessDefinition subProcessDefinition = repositoryService.getProcessDefinition(subProcessDefinitionId);
    assertEquals("simpleSubProcess", subProcessDefinition.getKey());

    // sub process instance start initialized event
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(9);
    assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
    assertEquals(subProcessInstanceId, event.getProcessInstanceId());
    assertNotEquals(subProcessInstanceId, event.getExecutionId());
    subProcessDefinitionId = ((ExecutionEntity) event.getEntity()).getProcessDefinitionId();
    assertNotNull(subProcessDefinitionId);
    
    event = (ActivitiEngineEntityEvent) listener.getEventsReceived().get(10);
    assertEquals(FlowableEngineEventType.PROCESS_STARTED, event.getType());
    assertEquals(subProcessInstanceId, event.getProcessInstanceId());
    assertEquals(subProcessDefinitionId, event.getProcessDefinitionId());
    assertTrue(event instanceof FlowableProcessStartedEvent);
    assertEquals(processDefinitionId, ((FlowableProcessStartedEvent) event).getNestedProcessDefinitionId());
    assertEquals(processInstance.getId(), ((FlowableProcessStartedEvent) event).getNestedProcessInstanceId());

    listener.clearEventsReceived();
  }

  /**
   * Test process with signals start.
   */
  @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalWithGlobalScope.bpmn20.xml" })
  public void testSignalProcessInstanceStart() throws Exception {
    this.runtimeService.startProcessInstanceByKey("processWithSignalCatch");
    listener.clearEventsReceived();

    runtimeService.startProcessInstanceByKey("processWithSignalThrow");
    listener.clearEventsReceived();
  }

  /**
   * Test Start->End process on PROCESS_COMPLETED event
   */
  @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.noneTaskProcess.bpmn20.xml" })
  public void testProcessCompleted_StartEnd() throws Exception {
    this.runtimeService.startProcessInstanceByKey("noneTaskProcess");

    assertEquals("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.", 1, listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED).size());
  }

  /**
   * Test Start->User Task process on PROCESS_COMPLETED event
   */
  @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.noEndProcess.bpmn20.xml" })
  public void testProcessCompleted_NoEnd() throws Exception {
    ProcessInstance noEndProcess = this.runtimeService.startProcessInstanceByKey("noEndProcess");
    Task task = taskService.createTaskQuery().processInstanceId(noEndProcess.getId()).singleResult();
    taskService.complete(task.getId());

    assertEquals("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.", 1, listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED).size());
  }

  /**
   * Test +-->Task1 Start-<> +-->Task1
   * 
   * process on PROCESS_COMPLETED event
   */
  @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.parallelGatewayNoEndProcess.bpmn20.xml" })
  public void testProcessCompleted_ParallelGatewayNoEnd() throws Exception {
    this.runtimeService.startProcessInstanceByKey("noEndProcess");

    assertEquals("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.", 1, listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED).size());
  }

  /**
   * Test +-->End1 Start-<> +-->End2
   * <p/>
   * process on PROCESS_COMPLETED event
   */
  @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.parallelGatewayTwoEndsProcess.bpmn20.xml" })
  public void testProcessCompleted_ParallelGatewayTwoEnds() throws Exception {
    this.runtimeService.startProcessInstanceByKey("noEndProcess");

    List<FlowableEvent> events = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED);
    assertEquals("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.", 1, events.size());
  }

  @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testProcessInstanceCancelledEvents_cancel() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertNotNull(processInstance);
    listener.clearEventsReceived();

    runtimeService.deleteProcessInstance(processInstance.getId(), "delete_test");

    List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals("ActivitiEventType.PROCESS_CANCELLED was expected 1 time.", 1, processCancelledEvents.size());
    FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
    assertTrue("The cause has to be the same as deleteProcessInstance method call", FlowableCancelledEvent.class.isAssignableFrom(processCancelledEvent.getClass()));
    assertEquals("The process instance has to be the same as in deleteProcessInstance method call", processInstance.getId(), processCancelledEvent.getProcessInstanceId());
    assertEquals("The execution instance has to be the same as in deleteProcessInstance method call", processInstance.getId(), processCancelledEvent.getExecutionId());
    assertEquals("The cause has to be the same as in deleteProcessInstance method call", "delete_test", processCancelledEvent.getCause());

    List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertEquals("ActivitiEventType.ACTIVITY_CANCELLED was expected 1 time.", 1, taskCancelledEvents.size());
    FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) taskCancelledEvents.get(0);
    assertTrue("The cause has to be the same as deleteProcessInstance method call", FlowableActivityCancelledEvent.class.isAssignableFrom(activityCancelledEvent.getClass()));
    assertEquals("The process instance has to be the same as in deleteProcessInstance method call", processInstance.getId(), activityCancelledEvent.getProcessInstanceId());
    assertEquals("The cause has to be the same as in deleteProcessInstance method call", "delete_test", activityCancelledEvent.getCause());

    listener.clearEventsReceived();
  }

  @Deployment(resources = { "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
  public void testProcessInstanceCancelledEvents_cancelProcessHierarchy() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
    assertNotNull(processInstance);
    listener.clearEventsReceived();

    runtimeService.deleteProcessInstance(processInstance.getId(), "delete_test");

    List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals("ActivitiEventType.PROCESS_CANCELLED was expected 2 times.", 2, processCancelledEvents.size());
    FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
    assertTrue("The cause has to be the same as deleteProcessInstance method call", FlowableCancelledEvent.class.isAssignableFrom(processCancelledEvent.getClass()));
    assertEquals("The process instance has to be the same as in deleteProcessInstance method call", subProcess.getId(), processCancelledEvent.getProcessInstanceId());
    assertEquals("The execution instance has to be the same as in deleteProcessInstance method call", subProcess.getId(), processCancelledEvent.getExecutionId());
    assertEquals("The cause has to be the same as in deleteProcessInstance method call", "delete_test", processCancelledEvent.getCause());
    
    processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(1);
    assertTrue("The cause has to be the same as deleteProcessInstance method call", FlowableCancelledEvent.class.isAssignableFrom(processCancelledEvent.getClass()));
    assertEquals("The process instance has to be the same as in deleteProcessInstance method call", processInstance.getId(), processCancelledEvent.getProcessInstanceId());
    assertEquals("The execution instance has to be the same as in deleteProcessInstance method call", processInstance.getId(), processCancelledEvent.getExecutionId());
    assertEquals("The cause has to be the same as in deleteProcessInstance method call", "delete_test", processCancelledEvent.getCause());

    assertEquals("No task can be active for deleted process.", 0, this.taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());

    List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertEquals("ActivitiEventType.ACTIVITY_CANCELLED was expected 1 time.", 1, taskCancelledEvents.size());
    FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) taskCancelledEvents.get(0);
    assertTrue("The cause has to be the same as deleteProcessInstance method call", FlowableActivityCancelledEvent.class.isAssignableFrom(activityCancelledEvent.getClass()));
    assertEquals("The process instance has to point to the subprocess", subProcess.getId(), activityCancelledEvent.getProcessInstanceId());
    assertEquals("The cause has to be the same as in deleteProcessInstance method call", "delete_test", activityCancelledEvent.getCause());

    listener.clearEventsReceived();
  }

  @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testProcessInstanceCancelledEvents_complete() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertNotNull(processInstance);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());

    List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals("There should be no ActivitiEventType.PROCESS_CANCELLED event after process complete.", 0, processCancelledEvents.size());
    List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertEquals("There should be no ActivitiEventType.ACTIVITY_CANCELLED event.", 0, taskCancelledEvents.size());

  }

  @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testProcessInstanceTerminatedEvents_complete() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertNotNull(processInstance);

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.complete(task.getId());

    List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals("There should be no ActivitiEventType.PROCESS_TERMINATED event after process complete.", 0, processTerminatedEvents.size());
  }

  @Deployment(resources = "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testProcessTerminate.bpmn")
  public void testProcessInstanceTerminatedEvents() throws Exception {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertEquals(3, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
    taskService.complete(task.getId());

    List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
    assertEquals("There should be exactly one ActivitiEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.", 1, processTerminatedEvents.size());
    ActivitiEngineEntityEvent processCompletedEvent = (ActivitiEngineEntityEvent) processTerminatedEvents.get(0);
    assertThat(processCompletedEvent.getProcessInstanceId(), is(pi.getProcessInstanceId()));

    List<FlowableEvent> activityTerminatedEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertThat("There should be exactly two ActivitiEventType.ACTIVITY_CANCELLED event after the task complete.", activityTerminatedEvents.size(), is(2));

    for (FlowableEvent event : activityTerminatedEvents) {
      
      FlowableActivityCancelledEventImpl activityEvent = (FlowableActivityCancelledEventImpl) event;
      if (activityEvent.getActivityId().equals("preNormalTerminateTask")) {
        assertThat("The user task must be terminated", activityEvent.getActivityId(), is("preNormalTerminateTask"));
        assertThat("The cause must be terminate end event", ((FlowNode) activityEvent.getCause()).getId(), is("EndEvent_2"));
      } else if (activityEvent.getActivityId().equals("EndEvent_2")) {
        assertThat("The end event must be terminated", activityEvent.getActivityId(), is("EndEvent_2"));
        assertThat("The cause must be terminate end event", ((FlowNode) activityEvent.getCause()).getId(), is("EndEvent_2"));
      }
      
    }
    
  }

  @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
      "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn" })
  public void testProcessInstanceTerminatedEvents_callActivity() throws Exception {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    assertProcessEnded(pi.getId());
    List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
    assertEquals("There should be exactly one ActivitiEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.", 1, processTerminatedEvents.size());
    ActivitiEngineEntityEvent processCompletedEvent = (ActivitiEngineEntityEvent) processTerminatedEvents.get(0);
    assertNotEquals(pi.getProcessInstanceId(), processCompletedEvent.getProcessInstanceId());
    assertThat(processCompletedEvent.getProcessDefinitionId(), containsString("terminateEndEventSubprocessExample"));

  }

  @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInParentProcess.bpmn", "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  public void testProcessInstanceTerminatedEvents_terminateInParentProcess() throws Exception {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateParentProcess");

    // should terminate the called process and continue the parent
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
    taskService.complete(task.getId());

    assertProcessEnded(pi.getId());
    List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
    assertEquals("There should be exactly one ActivitiEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.", 1, processTerminatedEvents.size());
    ActivitiEngineEntityEvent processCompletedEvent = (ActivitiEngineEntityEvent) processTerminatedEvents.get(0);
    assertThat(processCompletedEvent.getProcessInstanceId(), is(pi.getProcessInstanceId()));
    assertThat(processCompletedEvent.getProcessDefinitionId(), containsString("terminateParentProcess"));

    List<FlowableEvent> activityTerminatedEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertThat("3 activities must be cancelled.", activityTerminatedEvents.size(), is(3));
    
    for (FlowableEvent event : activityTerminatedEvents) {
      
      FlowableActivityCancelledEventImpl activityEvent = (FlowableActivityCancelledEventImpl) event;
      
      if (activityEvent.getActivityId().equals("theTask")) {
        
        assertThat("The user task must be terminated in the called sub process.", activityEvent.getActivityId(), is("theTask"));
        assertThat("The cause must be terminate end event", ((FlowNode) activityEvent.getCause()).getId(), is("EndEvent_3"));
        
      } else if (activityEvent.getActivityId().equals("CallActivity_1")) {
        
        assertThat("The call activity must be terminated", activityEvent.getActivityId(), is("CallActivity_1"));
        assertThat("The cause must be terminate end event", ((FlowNode) activityEvent.getCause()).getId(), is("EndEvent_3"));
        
      } else if (activityEvent.getActivityId().equals("EndEvent_3")) {
        
        assertThat("The end event must be terminated", activityEvent.getActivityId(), is("EndEvent_3"));
        assertThat("The cause must be terminate end event", ((FlowNode) activityEvent.getCause()).getId(), is("EndEvent_3"));
        
      }
      
    }
    
  }

  @Deployment(resources = {
          "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
          "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  public void testProcessCompletedEvents_callActivityErrorEndEvent() throws Exception {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity");

    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("Task in subprocess", task.getName());
    List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).list();
    assertEquals(1, subProcesses.size());

    // Completing the task will reach the end error event,
    // which is caught on the call activity boundary
    taskService.complete(task.getId());

    List<FlowableEvent> processCompletedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT);
    assertEquals("There should be exactly an ActivitiEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT event after the task complete.", 1, processCompletedEvents.size());
    ActivitiEngineEntityEvent processCompletedEvent = (ActivitiEngineEntityEvent) processCompletedEvents.get(0);
    assertEquals(subProcesses.get(0).getId(), processCompletedEvent.getExecutionId());

    task = taskService.createTaskQuery().singleResult();
    assertEquals("Escalated Task", task.getName());

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    assertProcessEnded(pi.getId());
  }
  
  @Deployment(resources = {
      "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
      "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml"})
  public void testDeleteMultiInstanceCallActivityProcessInstance() {
    assertEquals(0, taskService.createTaskQuery().count());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelCallActivity");
    assertEquals(7, runtimeService.createProcessInstanceQuery().count());
    assertEquals(12, taskService.createTaskQuery().count());
    this.listener.clearEventsReceived();
    
    runtimeService.deleteProcessInstance(processInstance.getId(), "testing instance deletion");
    
    assertEquals("Task cancelled event has to be fired.", this.listener.getEventsReceived().get(0).getType(), FlowableEngineEventType.ACTIVITY_CANCELLED);
    assertEquals("SubProcess cancelled event has to be fired.", this.listener.getEventsReceived().get(2).getType(), FlowableEngineEventType.PROCESS_CANCELLED);
    assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    assertEquals(0, taskService.createTaskQuery().count());
  }

  @Override
  protected void initializeServices() {
    super.initializeServices();
    this.listener = new TestInitializedEntityEventListener();
    processEngineConfiguration.getEventDispatcher().addEventListener(this.listener);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    if (listener != null) {
      listener.clearEventsReceived();
      processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }
  }

  private class TestInitializedEntityEventListener implements FlowableEventListener {

    private List<FlowableEvent> eventsReceived;

    public TestInitializedEntityEventListener() {

      eventsReceived = new ArrayList<FlowableEvent>();
    }

    public List<FlowableEvent> getEventsReceived() {
      return eventsReceived;
    }

    public void clearEventsReceived() {
      eventsReceived.clear();
    }

    @Override
    public void onEvent(FlowableEvent event) {
      if (event instanceof FlowableEntityEvent && ProcessInstance.class.isAssignableFrom(((FlowableEntityEvent) event).getEntity().getClass())) {
        // check whether entity in the event is initialized before
        // adding to the list.
        assertNotNull(((ExecutionEntity) ((FlowableEntityEvent) event).getEntity()).getId());
        eventsReceived.add(event);
      } else if (FlowableEngineEventType.PROCESS_CANCELLED.equals(event.getType()) || FlowableEngineEventType.ACTIVITY_CANCELLED.equals(event.getType())) {
        eventsReceived.add(event);
      }
    }

    @Override
    public boolean isFailOnException() {
      return true;
    }

    public List<FlowableEvent> filterEvents(FlowableEngineEventType eventType) {// count
                                                                          // timer
                                                                          // cancelled
                                                                          // events
      List<FlowableEvent> filteredEvents = new ArrayList<FlowableEvent>();
      List<FlowableEvent> eventsReceived = listener.getEventsReceived();
      for (FlowableEvent eventReceived : eventsReceived) {
        if (eventType.equals(eventReceived.getType())) {
          filteredEvents.add(eventReceived);
        }
      }
      return filteredEvents;
    }

  }
}