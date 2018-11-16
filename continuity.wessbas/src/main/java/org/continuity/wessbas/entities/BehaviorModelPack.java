package org.continuity.wessbas.entities;

import java.nio.file.Path;

import org.continuity.api.entities.artifact.SessionsBundlePack;

/**
 * Pack that holds sessions for each user group and the path to the created behavior model files.
 * @author Alper Hidiroglu
 *
 */
public class BehaviorModelPack {

	/**
	 * Sessions for each user group.
	 */
	private SessionsBundlePack sessionsBundlePack;
	
	/**
	 * Path to behavior model files. 
	 */
	private Path pathToBehaviorModelFiles;
	
	public BehaviorModelPack(SessionsBundlePack sessionsBundlePack, Path pathToBehaviorModelFiles) {
		this.sessionsBundlePack = sessionsBundlePack;
		this.pathToBehaviorModelFiles = pathToBehaviorModelFiles;
	}
	
	public BehaviorModelPack() {
		
	}
	
	public SessionsBundlePack getSessionsBundlePack() {
		return sessionsBundlePack;
	}

	public void setSessionsBundlePack(SessionsBundlePack sessionsBundlePack) {
		this.sessionsBundlePack = sessionsBundlePack;
	}

	public Path getPathToBehaviorModelFiles() {
		return pathToBehaviorModelFiles;
	}

	public void setPathToBehaviorModelFiles(Path pathToBehaviorModelFiles) {
		this.pathToBehaviorModelFiles = pathToBehaviorModelFiles;
	}

}
