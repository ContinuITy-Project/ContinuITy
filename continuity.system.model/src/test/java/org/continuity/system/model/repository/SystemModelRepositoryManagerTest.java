package org.continuity.system.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.system.model.SystemModelTestInstance;
import org.continuity.system.model.entities.ModelElementReference;
import org.continuity.system.model.entities.SystemChange;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Henning Schulz
 *
 */
public class SystemModelRepositoryManagerTest {

	private SystemModelRepository repositoryMock;

	private SystemModelRepositoryManager manager;

	@Before
	public void setup() {
		repositoryMock = Mockito.mock(SystemModelRepository.class);
		manager = new SystemModelRepositoryManager(repositoryMock);
	}

	@Test
	public void testSaveSameModel() {
		testWithSameModel(SystemModelTestInstance.FIRST.get());
		testWithSameModel(SystemModelTestInstance.SECOND.get());
		testWithSameModel(SystemModelTestInstance.THIRD.get());
	}

	private void testWithSameModel(SystemModel systemModel) {
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(systemModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", systemModel);
		assertThat(extractChanges(report.getChanges())).as("Expect the changes of the report to be empty.").isEmpty();
		assertThat(extractChanges(report.getIgnoredChanges())).as("Expect the ignored changes of the report to be empty.").isEmpty();
	}

	@Test
	public void testSaveModelWithAddedInterface() throws IOException {
		SystemModel firstModel = SystemModelTestInstance.FIRST.get();
		SystemModel secondModel = SystemModelTestInstance.SECOND.get();
		SystemModel thirdModel = SystemModelTestInstance.THIRD.get();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", secondModel);
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added").containsExactly("logout");
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
		.as("Expected that there are no other changes than the addition of logout").isEmpty();
		assertThat(extractChanges(report.getIgnoredChanges())).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<SystemModel> modelCaptor = ArgumentCaptor.forClass(SystemModel.class);
		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue()).as("Expected the second model to be stored").isEqualTo(secondModel);

		// Ignoring INTERFACE_ADDED
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", secondModel, EnumSet.of(SystemChangeType.INTERFACE_ADDED));
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
		.as("Expected that there are no other ignored changes than the addition of logout").isEmpty();
		assertThat(extractChanges(report.getChanges())).as("Expect the changes of the report to be empty.").isEmpty();

		Mockito.verify(repositoryMock, Mockito.times(0)).save(Mockito.anyString(), Mockito.any());

		// Removing an interface at the same time
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_ADDED));
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
		.as("Expected that there are no other ignored changes than the addition of logout").isEmpty();
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected the login interface to be removed").containsExactly("login");
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getReferenced)
		.as("Expected that there are no other changes than the removal of login").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getInterfaces()).as("Expected the stored model to be empty").isEmpty();
	}

	@Test
	public void testSaveModelWithRemovedInterface() throws IOException {
		SystemModel firstModel = SystemModelTestInstance.FIRST.get();
		SystemModel secondModel = SystemModelTestInstance.SECOND.get();
		SystemModel thirdModel = SystemModelTestInstance.THIRD.get();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(secondModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel);
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed").containsExactly("login");
		assertThat(extractChanges(report.getIgnoredChanges())).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<SystemModel> modelCaptor = ArgumentCaptor.forClass(SystemModel.class);
		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue()).as("Expected the third model to be stored").isEqualTo(thirdModel);

		// Cannot check for absence of other changes, since the user parameter was added

		// Ignoring INTERFACE_REMOVED
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(secondModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_REMOVED));
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED))
		.as("Expected that there are no other ignored changes than the removal of login").isEmpty();

		// Adding an interface at the same time
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_REMOVED));
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(extractChanges(report.getIgnoredChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED))
		.as("Expected that there are no other ignored changes than the removal of login").isEmpty();
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getReferenced)
		.extracting(ModelElementReference::getId).as("Expected the logout interface to be added").containsExactly("logout");
		assertThat(extractChanges(report.getChanges()).filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getReferenced)
		.as("Expected that there are no other changes than the addition of logout").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getInterfaces()).extracting(ServiceInterface::getId).as("Expected the stored model to contain exactly the interfaces login and logout")
		.containsExactlyInAnyOrder("login", "logout");
	}

	private Stream<SystemChange> extractChanges(Map<ModelElementReference, Set<SystemChange>> changeMap) {
		return changeMap.entrySet().stream().map(Map.Entry::getValue).flatMap(Set::stream);
	}

}
