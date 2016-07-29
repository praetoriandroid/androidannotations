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

import com.sun.codemodel.*;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.api.Lazy;
import org.androidannotations.api.LazyImpl;
import org.androidannotations.helper.TargetAnnotationHelper;
import org.androidannotations.holder.EBeanHolder;
import org.androidannotations.holder.EComponentHolder;
import org.androidannotations.model.AnnotationElements;
import org.androidannotations.process.IsValid;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static com.sun.codemodel.JExpr.*;

public class BeanHandler extends BaseAnnotationHandler<EComponentHolder> {

	private final TargetAnnotationHelper annotationHelper;

	public BeanHandler(ProcessingEnvironment processingEnvironment) {
		super(Bean.class, processingEnvironment);
		annotationHelper = new TargetAnnotationHelper(processingEnv, getTarget());
	}

	@Override
	public void validate(Element element, AnnotationElements validatedElements, IsValid valid) {
		validatorHelper.enclosingElementHasEnhancedComponentAnnotation(element, validatedElements, valid);

		validatorHelper.isNotPrivate(element, valid);

		if (isLazy (element)) {
			TypeMirror lazyElement = extractLazyBean(element);
			if (lazyElement == null) {
				valid.invalidate();
				annotationHelper.printAnnotationError(element, "The Lazy field should have exactly one param");
				return;
			}
			//TODO
			return;
		}

		validatorHelper.typeOrTargetValueHasAnnotation(EBean.class, element, valid);
	}

	@Override
	public void preProcess(Element element, EComponentHolder holder) throws Exception {
		super.preProcess(element, holder);

		if (!isLazy(element)) {
			EBeanHolder injectedClassHolder = getGeneratedClassHolder(element);
			JBlock block = holder.getInitBody();
			injectedClassHolder.invokeLockInject(block);
		}
	}

	@Override
	public void process(Element element, EComponentHolder holder) throws Exception {
		String typeQualifiedName = getTypeQualifiedName(element);
		JClass injectedClass = refClass(getGeneratedClassName(typeQualifiedName));

		String fieldName = element.getSimpleName().toString();
		JFieldRef beanField = ref(JExpr._this(), fieldName);
		JBlock block = holder.getInitBody();

		boolean hasNonConfigurationInstanceAnnotation = element.getAnnotation(NonConfigurationInstance.class) != null;
		if (hasNonConfigurationInstanceAnnotation) {
			block = block._if(beanField.eq(_null()))._then();
		}

		if (isLazy(element)) {
			JClass clazz = refClass(typeQualifiedName);
			JClass narrowLazy = refClass(LazyImpl.class).narrow(clazz);
			JDefinedClass anonymousClass = codeModel().anonymousClass(narrowLazy);
			JMethod create = anonymousClass.method(JMod.PUBLIC, clazz, "create");
			create.annotate(Override.class);
			create.body()._return(getInstance(holder, injectedClass));
			block.assign(beanField, _new(anonymousClass));
		} else {
			JInvocation getInstance = getInstance(holder, injectedClass);
			block.assign(beanField, getInstance);
		}
	}

	@Override
	public void postProcess(Element element, EComponentHolder holder) throws Exception {
		super.postProcess(element, holder);

		if (!isLazy(element)) {
			EBeanHolder injectedClassHolder = getGeneratedClassHolder(element);
			String typeQualifiedName = getTypeQualifiedName(element);
			JClass injectedClass = refClass(getGeneratedClassName(typeQualifiedName));
			String fieldName = element.getSimpleName().toString();
			JFieldRef beanField = ref(JExpr._this(), fieldName);
			injectedClassHolder.invokeUnlockInject(holder.getInitBody(), cast(injectedClass, beanField));
		}
	}

	private EBeanHolder getGeneratedClassHolder(Element element) {
		String typeQualifiedName = getTypeQualifiedName(element);
		String generatedClassName = getGeneratedClassName(typeQualifiedName);
		EBeanHolder generatedClassHolder = (EBeanHolder) processHolder.getGeneratedClassHolder(generatedClassName);
		if (generatedClassHolder == null) {
			throw new NullPointerException("Holder not found: typeQualifiedName=" + typeQualifiedName + ", generatedClassName=" + generatedClassName);
		}
		return generatedClassHolder;
	}

	private String getTypeQualifiedName(Element element) {
		TypeMirror typeMirror = annotationHelper.extractAnnotationClassParameter(element);
		if (typeMirror == null) {
			typeMirror = element.asType();
			if (isLazy(element)) {
				typeMirror = extractLazyBean(element);
			} else {
				typeMirror = annotationHelper.getTypeUtils().erasure(typeMirror);
			}
		}

		return typeMirror.toString();
	}

	private String getGeneratedClassName(String typeQualifiedName) {
		return annotationHelper.generatedClassQualifiedNameFromQualifiedName(typeQualifiedName);
	}

	private JInvocation getInstance(EComponentHolder holder, JClass injectedClass) {
		return injectedClass.staticInvoke(EBeanHolder.GET_INSTANCE_METHOD_NAME).arg(holder.getContextRef());
	}

	private boolean isLazy(Element element) {
		TypeMirror typeMirror = element.asType();
		typeMirror = annotationHelper.getTypeUtils().erasure(typeMirror);
		String typeQualifiedName = typeMirror.toString();
		return typeQualifiedName.equals(Lazy.class.getName());
	}

	private TypeMirror extractLazyBean(Element element) {
		DeclaredType typeMirror = (DeclaredType) element.asType();
		List<? extends TypeMirror> types = typeMirror.getTypeArguments();
		if (types.size() != 1) {
			return null;
		}
		return types.get(0);
	}
}
