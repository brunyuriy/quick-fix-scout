/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {

	public JavaReplaceWithEditionAction() {
	}

	@Override
	protected JavaHistoryActionImpl createDelegate() {
		return new JavaReplaceWithEditionActionImpl(false);
	}
}
