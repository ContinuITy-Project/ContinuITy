package org.continuity.idpa.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;

import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.entities.report.ModelElementReference;
import org.continuity.idpa.AppId;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.StaticIdpaTestInstance;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Henning Schulz
 *
 */
public class SystemModelRepositoryManagerTest {

	private IdpaStorage repositoryMock;

	private ApplicationStorageManager manager;

	@Before
	public void setup() {
		repositoryMock = Mockito.mock(IdpaStorage.class);
		manager = new ApplicationStorageManager(repositoryMock);
	}

	@Test
	public void testSaveSameModel() {
		testWithSameModel(StaticIdpaTestInstance.FIRST.getApplication());
		testWithSameModel(StaticIdpaTestInstance.SECOND.getApplication());
		testWithSameModel(StaticIdpaTestInstance.THIRD.getApplication());
	}

	private void testWithSameModel(Application systemModel) {
		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, systemModel, null));

		ApplicationChangeReport report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), systemModel);
		assertThat(report.getApplicationChanges()).as("Expect the changes of the report to be empty.").isEmpty();
		assertThat(report.getIgnoredApplicationChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();
	}

	@Test
	public void testSaveModelWithAddedInterface() throws IOException {
		Application firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		Application secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		Application thirdModel = StaticIdpaTestInstance.THIRD.getApplication();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, firstModel, null));

		ApplicationChangeReport report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), secondModel);
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_ADDED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added").containsExactly("logout");
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_ADDED))
				.as("Expected that (except for the addition of logout) the parameters user and logoutuser are added as only changes.").extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).containsExactlyInAnyOrder("logoutuser", "user");
		assertThat(report.getIgnoredApplicationChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq(AppId.fromString("SystemModelRepositoryManagerTest")), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getTimestamp()).as("Expected the date of the second model").isEqualTo(secondModel.getTimestamp());
		assertThat(modelCaptor.getValue().getEndpoints()).as("Expected the endpoints of the second model").isEqualTo(secondModel.getEndpoints());

		// Ignoring INTERFACE_ADDED
		firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		thirdModel = StaticIdpaTestInstance.THIRD.getApplication();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, firstModel, null));

		report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), secondModel,
				EnumSet.of(ApplicationChangeType.ENDPOINT_ADDED, ApplicationChangeType.PARAMETER_ADDED, ApplicationChangeType.PARAMETER_REMOVED));
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_ADDED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_ADDED))
				.as("Expected that (except for the addition of logout) the parameters user and logoutuser are added as only ignored changes.").extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).containsExactlyInAnyOrder("logoutuser", "user");
		assertThat(report.getApplicationChanges()).as("Expect the changes of the report to be empty.").isEmpty();

		Mockito.verify(repositoryMock, Mockito.times(0)).save(Mockito.any(), Mockito.any(Application.class));

		// Removing an interface at the same time
		firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		thirdModel = StaticIdpaTestInstance.THIRD.getApplication();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, firstModel, null));

		report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), thirdModel, EnumSet.of(ApplicationChangeType.ENDPOINT_ADDED));
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_ADDED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_ADDED))
				.as("Expected that there are no other ignored changes than the addition of logout").isEmpty();
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_REMOVED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected the login interface to be removed").containsExactly("login");
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_REMOVED)).extracting(ApplicationChange::getChangedElement)
				.as("Expected that there are no other changes than the removal of login").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq(AppId.fromString("SystemModelRepositoryManagerTest")), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getEndpoints()).as("Expected the stored model to be empty").isEmpty();
	}

	@Test
	public void testSaveModelWithRemovedInterface() throws IOException {
		Application firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		Application secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		Application thirdModel = StaticIdpaTestInstance.THIRD.getApplication();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, secondModel, null));

		ApplicationChangeReport report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), thirdModel);
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_REMOVED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed").containsExactly("login");
		assertThat(report.getIgnoredApplicationChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq(AppId.fromString("SystemModelRepositoryManagerTest")), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getTimestamp()).as("Expected the date of the third model").isEqualTo(thirdModel.getTimestamp());
		assertThat(modelCaptor.getValue().getEndpoints()).as("Expected the endpoints of the third model").isEqualTo(thirdModel.getEndpoints());

		// Cannot check for absence of other changes, since the user parameter was added

		// Ignoring INTERFACE_REMOVED
		firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		thirdModel = StaticIdpaTestInstance.THIRD.getApplication();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, secondModel, null));

		report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), thirdModel, EnumSet.of(ApplicationChangeType.ENDPOINT_REMOVED));
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_REMOVED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_REMOVED))
				.as("Expected that there are no other ignored changes than the removal of login").isEmpty();

		// Adding an interface at the same time
		firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		secondModel = StaticIdpaTestInstance.SECOND.getApplication();
		thirdModel = StaticIdpaTestInstance.THIRD.getApplication();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, firstModel, null));

		report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), thirdModel, EnumSet.of(ApplicationChangeType.ENDPOINT_REMOVED));
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_REMOVED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(report.getIgnoredApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_REMOVED))
				.as("Expected that there are no other ignored changes than the removal of login").isEmpty();
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() == ApplicationChangeType.ENDPOINT_ADDED)).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).as("Expected the logout interface to be added").containsExactly("logout");
		assertThat(report.getApplicationChanges().stream().filter(change -> change.getType() != ApplicationChangeType.ENDPOINT_ADDED)).extracting(ApplicationChange::getChangedElement)
				.as("Expected that there are no other changes than the addition of logout").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq(AppId.fromString("SystemModelRepositoryManagerTest")), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getEndpoints()).extracting(Endpoint::getId).as("Expected the stored model to contain exactly the interfaces login and logout")
				.containsExactlyInAnyOrder("login", "logout");
	}

	@Test
	public void testWithIgnoredParameterRemovals() throws IOException {
		Application firstModel = StaticIdpaTestInstance.FIRST.getApplication();
		Application secondModel = StaticIdpaTestInstance.SECOND.getApplication();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.any(), Mockito.any())).thenReturn(new Idpa(null, firstModel, null));
		ApplicationChangeReport report = manager.saveOrUpdate(AppId.fromString("SystemModelRepositoryManagerTest"), secondModel, EnumSet.of(ApplicationChangeType.PARAMETER_REMOVED));

		assertThat(report.getApplicationChanges()).filteredOn(change -> change.getType() == ApplicationChangeType.PARAMETER_ADDED).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).containsExactly("logoutuser");

		assertThat(report.getIgnoredApplicationChanges()).filteredOn(change -> change.getType() == ApplicationChangeType.PARAMETER_REMOVED).extracting(ApplicationChange::getChangedElement)
				.extracting(ModelElementReference::getId).containsExactly("user");

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq(AppId.fromString("SystemModelRepositoryManagerTest")), modelCaptor.capture());

		assertThat(modelCaptor.getValue().getEndpoints()).filteredOn(interf -> "login".equals(interf.getId())).extracting(interf -> (HttpEndpoint) interf).flatExtracting(Endpoint::getParameters)
				.extracting(Parameter::getId).containsExactlyInAnyOrder("user", "logoutuser");
	}

}
