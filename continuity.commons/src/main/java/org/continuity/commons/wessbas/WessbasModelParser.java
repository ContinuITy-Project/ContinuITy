package org.continuity.commons.wessbas;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import m4jdsl.WorkloadModel;
import m4jdsl.impl.M4jdslPackageImpl;

/**
 * Utility for parsing a WESSBAS {@link WorkloadModel} from an .xmi file.
 *
 * @author Eike Schulz (esc@informatik.uni-kiel.de), Henning Schulz
 *
 */
public class WessbasModelParser {

	/**
	 * Reads a {@link WorkloadModel} from an .xmi file.
	 *
	 * @param filename
	 *            The name of the file to be parsed.
	 * @return The parsed {@link WorkloadModel}.
	 * @throws IOException
	 *             If any error while reading occurs.
	 */
	public WorkloadModel readWorkloadModel(String filename) throws IOException {
		M4jdslPackageImpl.init();
		EObject eobject = readEcoreFromFile(filename, "xmi");

		if (eobject instanceof WorkloadModel) {
			return (WorkloadModel) eobject;
		} else {
			throw new IOException("The found ecore model has type " + eobject.getClass() + ". Expected m4js.WorkloadModel!");
		}
	}

	public WorkloadModel readWorkloadModel(InputStream inputStream) throws IOException {
		M4jdslPackageImpl.init();
		Resource resource = new XMIResourceImpl();
		resource.load(inputStream, null);
		EObject eobject = resource.getContents().get(0);

		if (eobject instanceof WorkloadModel) {
			return (WorkloadModel) eobject;
		} else {
			throw new IOException("The found ecore model has type " + eobject.getClass() + ". Expected m4js.WorkloadModel!");
		}
	}

	/**
	 * Reads an Ecore-model from an XMI-file and optionally registers a file extension in the XMI
	 * resource factory.
	 *
	 * <p>
	 * <u>Important:</u> the model-related Ecore package must have been initialized before!
	 *
	 * @param xmiFile
	 *            XMI-file to be read.
	 * @param extension
	 *            file extension to be optionally registered in the XMI resource factory; this might
	 *            be even <code>null</code>, if no extension shall be registered.
	 *
	 * @return the Ecore-model which has been read from the specified XMI-file.
	 *
	 * @throws IOException
	 *             if any error while reading occurs.
	 */
	public EObject readEcoreFromFile(final String xmiFile, final String extension) throws IOException {

		// to be returned;
		final EObject model;

		// resource to be read;
		final Resource resource;

		if (extension != null) {

			// register extension in the XMI resource factory;
			// might throw a NullPointerException (should never happen here);
			this.registerExtension(extension);
		}

		// might throw an IOException;
		resource = this.readResource(xmiFile);

		// first model element must be of type WorkloadModel;
		// might throw an IndexOutOfBoundsException (should never happen here);
		model = resource.getContents().get(0);

		return model;
	}

	/**
	 * Registers a file extension in the XMI resource factory.
	 *
	 * @param extension
	 *            file extension to be registered.
	 *
	 * @throws NullPointerException
	 *             if <code>null</code> has been passed as <code>extension</code>.
	 */
	private void registerExtension(final String extension) throws NullPointerException {

		final Resource.Factory.Registry registry = Resource.Factory.Registry.INSTANCE;

		final Map<String, Object> map = registry.getExtensionToFactoryMap();

		// might throw an UnsupportedOperation-, ClassCast-, NullPointer- or
		// IllegalArgumentException (anything else but a NullPointerException
		// should never be thrown here, since "extension" is of type String);
		map.put(extension, new XMIResourceFactoryImpl());
	}

	/**
	 * Reads an XMI-file from a given location.
	 *
	 * @param xmiFile
	 *            location of the XMI-file to be read.
	 *
	 * @return the content which has been read from file.
	 *
	 * @throws IOException
	 *             if any error while reading occurs.
	 */
	private Resource readResource(final String xmiFile) throws IOException {

		final Resource resource; // to be returned;

		final ResourceSet resourceSet = new ResourceSetImpl();

		try {

			// might throw an IllegalArgumentException;
			final URI uri = URI.createURI(xmiFile);

			// read resource with "loadOnDemand" being true;
			// might throw a Runtime- or WrappedException;
			resource = resourceSet.getResource(uri, true);

		} catch (final Exception ex) {

			final String message = String.format("Could not read resource \"%s\": %s", xmiFile, ex.getMessage());

			throw new IOException(message);
		}

		return resource;
	}

}
