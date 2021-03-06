package io.toolisticon.spiap.processor;


import io.toolisticon.annotationprocessortoolkit.tools.MessagerUtils;
import io.toolisticon.compiletesting.CompileTestBuilder;
import io.toolisticon.compiletesting.JavaFileObjectUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link SpiProcessor}.
 */
public class ServiceProcessorTest {


    @Before
    public void init() {
        MessagerUtils.setPrintMessageCodes(true);
    }


    CompileTestBuilder.CompilationTestBuilder compileTestBuilder = CompileTestBuilder
            .compilationTest()
            .addProcessors(ServiceProcessor.class);

    @Test
    public void test_validUsage() {

        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseValidUsage.java"))
                .compilationShouldSucceed()
                .testCompilation();
    }


    @Test
    public void test_annotationMustBePlacedOnClass() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseUsageOnInterface.java"))
                .compilationShouldFail()
                .expectedErrorMessages(ServiceProcessorMessages.ERROR_SPI_ANNOTATION_MUST_BE_PLACED_ON_CLASS.getCode())
                .testCompilation();
    }


    @Test
    public void test_annotationValueAttributeMustOnlyContainInterfaces() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseValueAttributeMustOnlyContainInterfaces.java"))
                .compilationShouldFail()
                .expectedErrorMessages(ServiceProcessorMessages.ERROR_VALUE_ATTRIBUTE_MUST_ONLY_CONTAIN_INTERFACES.getCode())
                .testCompilation();
    }

    @Test
    public void test_annotatedTypeMustImplementConfiguredInterfaces() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseMustImplementAnnotatedInterface.java"))
                .compilationShouldFail()
                .expectedErrorMessages(ServiceProcessorMessages.ERROR_ANNOTATED_CLASS_MUST_IMPLEMENT_CONFIGURED_INTERFACES.getCode())
                .testCompilation();
    }

    @Test
    public void test_processingShouldSucceesWithPlainInterfaces() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseValidUseWithPlainInterface.java"))
                .compilationShouldSucceed()
                .testCompilation();
    }

    //--
    @Test
    public void test_multipleServicesImplemented() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseMultipleServices.java"))
                .compilationShouldSucceed()
                .testCompilation();
    }


    @Test
    public void test_OutOfServiceAnnotatedServicesShouldntBeProcessed() {
        compileTestBuilder
                .addSources(JavaFileObjectUtils.readFromResource("serviceprocessor/TestcaseDontProcessOutOfServiceService.java"))
                .expectedNoteMessages(ServiceProcessorMessages.INFO_SKIP_ELEMENT_ANNOTATED_AS_OUT_OF_SERVICE.getCode())
                .compilationShouldSucceed()
                .testCompilation();

    }


}

