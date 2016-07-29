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
import static com.sun.codemodel.JExpr.invoke;
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static org.androidannotations.helper.ModelConstants.GENERATION_SUFFIX;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JStatement;
import org.androidannotations.api.BackgroundExecutor;
import org.androidannotations.api.UiThreadGetter;
import org.androidannotations.process.ProcessHolder;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;

public class EBeanHolder extends EComponentWithViewSupportHolder {

	private static final String GET_INSTANCE_PREFIX = "getInstance";
	private static final String GET_INSTANCE_INTERNAL_METHOD_NAME = GET_INSTANCE_PREFIX + "Internal" + GENERATION_SUFFIX;
	public static final String GET_INSTANCE_METHOD_NAME = GET_INSTANCE_PREFIX + GENERATION_SUFFIX;
	private static final String LOCK_INJECT_METHOD_NAME = "lockInject_";
	private static final String UNLOCK_INJECT_METHOD_NAME = "unlockInject_";
	private static final String AFTER_INJECT_METHOD_NAME = "afterInject_";

	private JClass runnableFutureGenericClass = refClass(RunnableFuture.class).narrow(generatedClass);
	private JClass futureTaskGenericClass = refClass(FutureTask.class).narrow(generatedClass);
	private JClass callableGenericClass = refClass(Callable.class).narrow(generatedClass);

	private JClass backgroundExecutorClass = refClass(BackgroundExecutor.class);
	private JClass uiThreadGetter = refClass(UiThreadGetter.class);

	private JFieldVar contextField;
	private JFieldVar instanceField;
	private JFieldVar lockCounterField;
	private JMethod constructor;
	private JMethod afterInjectMethod;

	private boolean hasSingletonScope;

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

	@Override
	public void addAfterInjectCall(String methodName) {
		getAfterInjectMethod().body().add(invoke(methodName));
	}

	public void invokeInitInConstructor() {
		JBlock constructorBody = constructor.body();
		constructorBody.invoke(getInit());
	}

	public void createFactoryMethod() {
		JFieldVar instanceField = null;

		if (hasSingletonScope) {
			instanceField = getInstanceField();
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
		factoryMethodBody._return(uiThreadGetter.staticInvoke("getOnUiThread").arg(runnableFuture));
	}

	private JFieldVar getInstanceField() {
		if (instanceField == null) {
			instanceField = generatedClass.field(PRIVATE | STATIC, generatedClass, "instance_");
		}
		return instanceField;
	}

	public void createInjectHelperMethods() {
		createLockInjectMethod();
		if (hasSingletonScope) {
			createSingletonUnlockInjectMethod();
		} else {
			createNonSingletonUnlockInjectMethod();
		}
		createAfterInjectMethod();
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

	private void createLockInjectMethod() {
		JMethod lockMethod = getLockInjectMethod();
		JBlock lockMethodBody = lockMethod.body();
		JFieldVar lockCounterField = getLockCounterField();
		lockMethodBody.add(new JExpressionStatement(JOp.incr(lockCounterField)));
	}

	private void createSingletonUnlockInjectMethod() {
		JMethod unlockMethod = getUnlockInjectMethod();
		JBlock unlockMethodBody = unlockMethod.body();
		JFieldVar lockCounterField = getLockCounterField();
		JFieldVar instanceField = getInstanceField();
		unlockMethodBody
				._if(JOp.eq(preDecr(lockCounterField), new JIntLiteralExpression(0)))
				._then().invoke(instanceField, AFTER_INJECT_METHOD_NAME);
	}

	private void createNonSingletonUnlockInjectMethod() {
		JMethod unlockMethod = getUnlockInjectMethod();
		JVar instance = unlockMethod.param(generatedClass, "instance");
		JBlock unlockMethodBody = unlockMethod.body();
		JFieldVar lockCounterField = getLockCounterField();
		unlockMethodBody
				._if(JOp.eq(preDecr(lockCounterField), new JIntLiteralExpression(0)))
				._then().invoke(instance, AFTER_INJECT_METHOD_NAME);
	}

	private void createAfterInjectMethod() {
		getAfterInjectMethod();
	}

	private JFieldVar getLockCounterField() {
		if (lockCounterField == null) {
			lockCounterField = generatedClass.field(PRIVATE | STATIC, int.class, "lockCounter_");
		}
		return lockCounterField;
	}

	private JMethod getLockInjectMethod() {
		return generatedClass.method(PUBLIC | STATIC, void.class, LOCK_INJECT_METHOD_NAME);
	}

	private JMethod getUnlockInjectMethod() {
		return generatedClass.method(PUBLIC | STATIC, void.class, UNLOCK_INJECT_METHOD_NAME);
	}

	private JMethod getAfterInjectMethod() {
		if (afterInjectMethod == null) {
			afterInjectMethod = generatedClass.method(PRIVATE, void.class, AFTER_INJECT_METHOD_NAME);
		}
		return afterInjectMethod;
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

	private static JExpression preDecr(JExpression e) {
		return new PreUnaryOp("--", e);
	}

	public void invokeLockInject(JBlock block) {
		block.add(generatedClass.staticInvoke(LOCK_INJECT_METHOD_NAME));
	}

	public void invokeUnlockInject(JBlock block, JExpression instanceArg) {
		JInvocation unlockInvocation = generatedClass.staticInvoke(UNLOCK_INJECT_METHOD_NAME);
		block.add(unlockInvocation);
		if (!hasSingletonScope) {
			unlockInvocation.arg(instanceArg);
		}
	}

	public void setHasSingletonScope(boolean hasSingletonScope) {
		this.hasSingletonScope = hasSingletonScope;
	}

	private static class JExpressionStatement implements JStatement {
		private final JExpression expression;

		JExpressionStatement(JExpression expression) {
			this.expression = expression;
		}

		@Override
		public void state(JFormatter formatter) {
			formatter.g(expression).p(';').nl();
		}
	}

	private static class JIntLiteralExpression extends JExpressionImpl {
		public final int value;

		JIntLiteralExpression(int value) {
			this.value = value;
		}

		public void generate(JFormatter f) {
			f.p(Integer.toString(value));
		}
	}

	private static class PreUnaryOp extends JExpressionImpl {
		private final String op;
		private final JExpression e;

		PreUnaryOp(String op, JExpression e) {
			this.op = op;
			this.e = e;
		}

		public void generate(JFormatter f) {
			f.p(this.op).g(this.e);
		}
	}

}
