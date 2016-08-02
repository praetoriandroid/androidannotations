/**
 * Copyright (C) 2010-2015 eBusiness Information, Excilys Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.androidannotations.handler;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;
import org.androidannotations.annotations.EBean;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.model.AnnotationElements;
import org.androidannotations.process.IsValid;
import org.androidannotations.process.ProcessHolder;

public class EBeanHandler extends BaseGeneratingAnnotationHandler<EBeanHolder> {

	public EBeanHandler(ProcessingEnvironment processingEnvironment) {
		super(EBean.class, processingEnvironment);
	}

	@Override
	public EBeanHolder createGeneratedClassHolder(ProcessHolder processHolder, TypeElement annotatedComponent) throws Exception {
		return new EBeanHolder(processHolder, annotatedComponent);
	}

	@Override
	public void validate(Element element, AnnotationElements validatedElements, IsValid valid) {
		super.validate(element, validatedElements, valid);

		validatorHelper.isNotPrivate(element, valid);

		validatorHelper.isAbstractOrHasEmptyOrContextConstructor(element, valid);
	}

	@Override
	public void preProcess(Element element, EBeanHolder holder) throws Exception {
		super.preProcess(element, holder);

		holder.invokeLockInject(holder.getInitBody());
	}

	@Override
	public void process(Element element, EBeanHolder holder) {
		boolean hasSingletonScope = hasSingletonScope(element);

		holder.setHasSingletonScope(hasSingletonScope);
		holder.createFactoryMethod();
		holder.createLockedFactoryMethod();
		holder.createInjectHelperMethods();

		if (!hasSingletonScope) {
			holder.createRebindMethod();
		}
	}

	@Override
	public void postProcess(Element element, EBeanHolder holder) throws Exception {
		super.postProcess(element, holder);

		holder.invokeUnlockInject(holder.getInitBody());
	}

	private boolean hasSingletonScope(Element element) {
		EBean eBeanAnnotation = element.getAnnotation(EBean.class);
		EBean.Scope eBeanScope = eBeanAnnotation.scope();
		return eBeanScope == EBean.Scope.Singleton;
	}
}
