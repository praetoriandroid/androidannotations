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
package org.androidannotations.holder;

import static com.sun.codemodel.JExpr._new;
import static com.sun.codemodel.JExpr._null;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static com.sun.codemodel.JMod.VOLATILE;
import static org.androidannotations.helper.ModelConstants.GENERATION_SUFFIX;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JTryBlock;
import org.androidannotations.api.BackgroundExecutor;
import org.androidannotations.api.BeanInstantiationException;
import org.androidannotations.process.ProcessHolder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;

public class EBeanHolder extends EComponentWithViewSupportHolder {

	private static final String GET_INSTANCE_PREFIX = "getInstance";
	private static final String GET_INSTANCE_INTERNAL_METHOD_NAME = GET_INSTANCE_PREFIX + "Internal" + GENERATION_SUFFIX;
	public static final String GET_INSTANCE_METHOD_NAME = GET_INSTANCE_PREFIX + GENERATION_SUFFIX;

	private JClass runnableFutureGenericClass = refClass(RunnableFuture.class).narrow(generatedClass);
	private JClass futureTaskGenericClass = refClass(FutureTask.class).narrow(generatedClass);
	private JClass callableGenericClass = refClass(Callable.class).narrow(generatedClass);

	private JClass backgroundExecutorClass = refClass(BackgroundExecutor.class);
	private JClass interruptedExceptionClass = refClass(InterruptedException.class);
	private JClass illegalStateExceptionClass = refClass(IllegalStateException.class);
	private JClass executionExceptionClass = refClass(ExecutionException.class);
	private JClass beanInstantiationExceptionClass = refClass(BeanInstantiationException.class);

	private JFieldVar contextField;
	private JMethod constructor;

	public EBeanHolder(ProcessHolder processHolder, TypeElement annotatedElement) throws Exception {
		super(processHolder, annotatedElement);
		setConstructor();
	}

	private void setConstructor() {
		constructor = generatedClass.constructor(PRIVATE);
		JVar constructorContextParam = getContextParam(constructor);
		JBlock constructorBody = constructor.body();
		List<ExecutableElement> constructors = ElementFilter.constructorsIn(annotatedElement.getEnclosedElements());
		ExecutableElement superConstructor = constructors.get(0);
		if (superConstructor.getParameters().size() == 1) {
			constructorBody.invoke("super").arg(constructorContextParam);
		}
		constructorBody.assign(getContextField(), constructorContextParam);
	}

	public JFieldVar getContextField() {
		if (contextField == null) {
			contextField = generatedClass.field(PRIVATE, classes().CONTEXT, "context_");
		}
		return contextField;
	}

	@Override
	protected void setContextRef() {
		contextRef = getContextField();
	}

	@Override
	protected void setInit() {
		init = generatedClass.method(PRIVATE, processHolder.codeModel().VOID, "init_");
	}

	public void invokeInitInConstructor() {
		JBlock constructorBody = constructor.body();
		constructorBody.invoke(getInit());
	}

	public void createFactoryMethod(boolean hasSingletonScope) {
		JFieldVar instanceField = null;

		if (hasSingletonScope) {
			instanceField = generatedClass.field(PRIVATE | STATIC | VOLATILE, generatedClass, "instance_");
			createSingletonInternalFactoryMethod(instanceField);
		} else {
			createNonSingletonInternalFactoryMethod();
		}

		JMethod factoryMethod = generatedClass.method(PUBLIC | STATIC, generatedClass, GET_INSTANCE_METHOD_NAME);
		JBlock factoryMethodBody = factoryMethod.body();

		JBlock checkUiThreadBlock = factoryMethodBody
				._if(backgroundExecutorClass.staticInvoke("isUiThread"))
				._then();
		JInvocation internalFactoryMethodInvocation = generatedClass.staticInvoke(GET_INSTANCE_INTERNAL_METHOD_NAME);
		JVar factoryMethodContextParam = factoryMethod.param(FINAL, classes().CONTEXT, "context");
		internalFactoryMethodInvocation.arg(factoryMethodContextParam);
		checkUiThreadBlock._return(internalFactoryMethodInvocation);

		if (hasSingletonScope) {
			factoryMethodBody.directStatement("synchronized(" + generatedClass.name() + ".class)");
			JBlock synchronizedBlock = new JBlock(true, true);
			JBlock checkHasInstance = synchronizedBlock
					._if(instanceField.ne(_null()))
					._then();
			checkHasInstance._return(instanceField);
			factoryMethodBody.add(synchronizedBlock);
		}

		JDefinedClass anonymousCallableClass = codeModel().anonymousClass(callableGenericClass);
		JMethod callMethod = anonymousCallableClass.method(PUBLIC, generatedClass, "call");
		callMethod.annotate(Override.class);
		callMethod.body()._return(internalFactoryMethodInvocation);
		JInvocation futureInstantiation = _new(futureTaskGenericClass);
		JExpression callableInstantiation = _new(anonymousCallableClass);
		futureInstantiation.arg(callableInstantiation);
		JVar runnableFuture = factoryMethodBody.decl(runnableFutureGenericClass, "runnableFuture", futureInstantiation);

		JVar handler = getHandler();
		factoryMethodBody.add(handler.invoke("post").arg(runnableFuture));

		JTryBlock tryBlock = factoryMethodBody._try();
		tryBlock.body()._return(runnableFuture.invoke("get"));

		JCatchBlock catchBlock = tryBlock._catch(interruptedExceptionClass);
		JVar exception = catchBlock.param("e");
		catchBlock.body()._throw(_new(illegalStateExceptionClass).arg(exception));

		catchBlock = tryBlock._catch(executionExceptionClass);
		exception = catchBlock.param("e");
		catchBlock.body()._throw(_new(beanInstantiationExceptionClass).arg(exception));
	}

	private void createSingletonInternalFactoryMethod(JFieldVar instanceField) {
		JMethod factoryMethod = getInternalFactoryMethod();
		JVar factoryMethodContextParam = getContextParam(factoryMethod);
		JBlock factoryMethodBody = factoryMethod.body();

		JBlock creationBlock = factoryMethodBody //
                ._if(instanceField.eq(_null())) //
                ._then();

		JVar previousNotifier = viewNotifierHelper.replacePreviousNotifierWithNull(creationBlock);
		creationBlock.directStatement("synchronized(" + generatedClass.name() + ".class)");
		JBlock synchronizedBlock = new JBlock(true, true);
		JInvocation newInvocation = _new(generatedClass).arg(factoryMethodContextParam.invoke("getApplicationContext"));
		synchronizedBlock.assign(instanceField, newInvocation);
		synchronizedBlock.invoke(instanceField, getInit());
		creationBlock.add(synchronizedBlock);

		viewNotifierHelper.resetPreviousNotifier(creationBlock, previousNotifier);

		factoryMethodBody._return(instanceField);
	}

	private void createNonSingletonInternalFactoryMethod() {
		JMethod factoryMethod = getInternalFactoryMethod();
		JVar factoryMethodContextParam = getContextParam(factoryMethod);
		JBlock factoryMethodBody = factoryMethod.body();

		JInvocation newInvocation = _new(generatedClass).arg(factoryMethodContextParam);
		JVar instance = factoryMethodBody.decl(generatedClass, "instance", newInvocation);
		factoryMethodBody._return(instance);
	}

	private JMethod getInternalFactoryMethod() {
		return generatedClass.method(PRIVATE | STATIC, generatedClass, GET_INSTANCE_INTERNAL_METHOD_NAME);
	}

	private JVar getContextParam(JMethod method) {
		return method.param(classes().CONTEXT, "context");
	}

	public void createRebindMethod() {
		JMethod rebindMethod = generatedClass.method(PUBLIC, codeModel().VOID, "rebind");
		JVar contextParam = getContextParam(rebindMethod);
		JBlock body = rebindMethod.body();
		body.assign(getContextField(), contextParam);
		body.invoke(getInit());
	}
}
