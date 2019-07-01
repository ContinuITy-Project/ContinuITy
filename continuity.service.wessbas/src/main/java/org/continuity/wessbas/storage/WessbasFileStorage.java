package org.continuity.wessbas.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.continuity.commons.storage.FileStorage;
import org.continuity.wessbas.entities.WessbasBundle;

import com.fasterxml.jackson.databind.ObjectMapper;

import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslPackageImpl;
import wessbas.commons.util.XmiEcoreHandler;

public class WessbasFileStorage extends FileStorage<WessbasBundle> {

	private static final String FILE_WESSBAS = "wessbas.xmi";

	private static final String FILE_BUNDLE = "bundle.json";

	private final ObjectMapper mapper = new ObjectMapper();

	public WessbasFileStorage(Path storagePath) {
		super(storagePath, new WessbasBundle());

		M4jdslPackageImpl.init();
	}

	@Override
	protected void write(Path dirPath, String id, WessbasBundle entity) throws IOException {
		Path idDir = toIdDir(dirPath, id);
		idDir.toFile().mkdirs();

		if ((entity != null) && (entity.getWorkloadModel() != null) && (entity.getVersion() != null)) {
			// For reserving a slot, an empty bundle is passed. In this case, only the folder should
			// be created.

			XmiEcoreHandler.getInstance().ecoreToXMI(entity.getWorkloadModel(), idDir.resolve(FILE_WESSBAS).toString());

			mapper.writer().writeValue(idDir.resolve(FILE_BUNDLE).toFile(), entity);
		}
	}

	@Override
	protected WessbasBundle read(Path dirPath, String id) throws IOException {
		Path idDir = toIdDir(dirPath, id);

		if (idDir.toFile().exists()) {
			WorkloadModel workloadModel = (WorkloadModel) XmiEcoreHandler.getInstance().xmiToEcore(idDir.resolve(FILE_WESSBAS).toString());

			WessbasBundle bundle = mapper.readValue(idDir.resolve(FILE_BUNDLE).toFile(), WessbasBundle.class);
			bundle.setWorkloadModel(workloadModel);

			return bundle;
		}
		return null;
	}

	@Override
	protected boolean remove(Path dirPath, String id) throws IOException {
		File idDir = toIdDir(dirPath, id).toFile();

		if (idDir.exists()) {
			FileUtils.deleteDirectory(idDir);
			return true;
		} else {
			return false;
		}
	}

	private Path toIdDir(Path dirPath, String id) {
		return dirPath.resolve(id);
	}

}
