/*
 * Copyright 2014-2015 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.server.plugin.messaging;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;
import org.kaaproject.kaa.common.avro.AvroByteArrayConverter;
import org.kaaproject.kaa.common.avro.GenericAvroConverter;
import org.kaaproject.kaa.server.common.core.plugin.base.BasePluginContractInstance;
import org.kaaproject.kaa.server.common.core.plugin.base.BasePluginContractItemInfo;
import org.kaaproject.kaa.server.common.core.plugin.def.PluginContractDef;
import org.kaaproject.kaa.server.common.core.plugin.def.PluginContractItemDef;
import org.kaaproject.kaa.server.common.core.plugin.def.SdkApiFile;
import org.kaaproject.kaa.server.common.core.plugin.generator.AbstractSdkApiGenerator;
import org.kaaproject.kaa.server.common.core.plugin.generator.PluginAPIMethodSignatureGenerator;
import org.kaaproject.kaa.server.common.core.plugin.generator.PluginSdkApiGenerationContext;
import org.kaaproject.kaa.server.common.core.plugin.generator.SpecificPluginSdkApiGenerationContext;
import org.kaaproject.kaa.server.common.core.plugin.instance.PluginContractInstance;
import org.kaaproject.kaa.server.common.core.plugin.instance.PluginContractItemInfo;
import org.kaaproject.kaa.server.plugin.messaging.gen.Configuration;
import org.kaaproject.kaa.server.plugin.messaging.gen.ItemConfiguration;
import org.kaaproject.kaa.server.plugin.messaging.gen.test.ClassA;
import org.kaaproject.kaa.server.plugin.messaging.gen.test.ClassB;
import org.kaaproject.kaa.server.plugin.messaging.gen.test.ClassC;

public class EndpointMessagePluginGenerator extends AbstractSdkApiGenerator<Configuration> {

    /**
     * Generates method signatures for
     * {@link MessagingSDKContract#buildSendMsgDef()}.
     *
     * @author Bohdan Khablenko
     */
    private class SendMsgMethodSignatureGenerator implements PluginAPIMethodSignatureGenerator {

        private static final String NULL_PARAM_TYPE_TEMPLATE = "";
        private static final String NULL_RETURN_TYPE_TEMPLATE = "java.util.concurrent.Future<Void>";

        private static final String NON_NULL_PARAM_TYPE_TEMPLATE = "{0} param";
        private static final String NON_NULL_RETURN_TYPE_TEMPLATE = "java.util.concurrent.Future<{0}>";

        @Override
        public String generateMethodSignature(String methodName, String paramType, String returnType) {

            // Insert the arguments into corresponding templates
            if (paramType == null && returnType == null) {
                paramType = MessageFormat.format(NULL_PARAM_TYPE_TEMPLATE, paramType);
                returnType = MessageFormat.format(NULL_RETURN_TYPE_TEMPLATE, returnType);
            } else if (paramType != null && returnType == null) {
                paramType = MessageFormat.format(NON_NULL_PARAM_TYPE_TEMPLATE, paramType);
                returnType = MessageFormat.format(NULL_RETURN_TYPE_TEMPLATE, returnType);
            } else if (paramType != null && returnType != null) {
                paramType = MessageFormat.format(NON_NULL_PARAM_TYPE_TEMPLATE, paramType);
                returnType = MessageFormat.format(NON_NULL_RETURN_TYPE_TEMPLATE, returnType);
            } else if (paramType == null && returnType != null) {
                paramType = MessageFormat.format(NULL_PARAM_TYPE_TEMPLATE, paramType);
                returnType = MessageFormat.format(NON_NULL_RETURN_TYPE_TEMPLATE, returnType);
            }

            // Format method signature
            return MessageFormat.format(METHOD_SIGNATURE_TEMPLATE, returnType, methodName, paramType);
        }
    }

    /**
     * Generates method signatures for
     * {@link MessagingSDKContract#buildReceiveMsgDef()}.
     *
     * @author Bohdan Khablenko
     */
    private class ReceiveMsgMethodSignatureGenerator implements PluginAPIMethodSignatureGenerator {

        private static final String PARAM_TYPE_TEMPLATE = "{0}_MethodListener listener";
        private static final String RETURN_TYPE_TEMPLATE = "void";

        private static final String METHOD_LISTENER_NULL_PARAM_TYPE_TEMPLATE = "";
        private static final String METHOD_LISTENER_NULL_RETURN_TYPE_TEMPLATE = "void";

        private static final String METHOD_LISTENER_NON_NULL_PARAM_TYPE_TEMPLATE = "{0} listener";
        private static final String METHOD_LISTENER_NON_NULL_RETURN_TYPE_TEMPLATE = "{0}";

        private static final String METHOD_LISTENER_TEMPLATE_FILE = "templates/listener.template";
        private static final String METHOD_LISTENER_CLASS_NAME_TEMPLATE = "{0}_MethodListener";
        private static final String METHOD_LISTENER_FILE_NAME_TEMPLATE = METHOD_LISTENER_CLASS_NAME_TEMPLATE + ".java";

        private final Collection<SdkApiFile> sources;

        public ReceiveMsgMethodSignatureGenerator(Collection<SdkApiFile> sources) {
            this.sources = sources;
        }

        @Override
        public String generateMethodSignature(String methodName, String paramType, String returnType) {

            // Pick a parameter type template for the method listener class
            String listenerParamTypeTemplate;
            if (paramType == null) {
                listenerParamTypeTemplate = METHOD_LISTENER_NULL_PARAM_TYPE_TEMPLATE;
            } else {
                listenerParamTypeTemplate = METHOD_LISTENER_NON_NULL_PARAM_TYPE_TEMPLATE;
            }

            // Pick a return type template for the method listener class
            String listenerReturnTypeTemplate;
            if (returnType == null) {
                listenerReturnTypeTemplate = METHOD_LISTENER_NULL_RETURN_TYPE_TEMPLATE;
            } else {
                listenerReturnTypeTemplate = METHOD_LISTENER_NON_NULL_RETURN_TYPE_TEMPLATE;
            }

            // Generate the method listener class body
            String content = EndpointMessagePluginGenerator.this.readFileAsString(METHOD_LISTENER_TEMPLATE_FILE);
            content = content.replace(PACKAGE_NAME, EndpointMessagePluginGenerator.this.namespace);
            content = content.replace(CLASS_NAME, MessageFormat.format(METHOD_LISTENER_CLASS_NAME_TEMPLATE, methodName));
            content = content.replace(PARAM_TYPE, MessageFormat.format(listenerParamTypeTemplate, paramType));
            content = content.replace(RETURN_TYPE, MessageFormat.format(listenerReturnTypeTemplate, returnType));

            // Add the method listener class as a source file
            String fileName = MessageFormat.format(METHOD_LISTENER_FILE_NAME_TEMPLATE, methodName);
            byte[] fileData = content.getBytes();
            sources.add(new SdkApiFile(fileName, fileData));

            paramType = MessageFormat.format(PARAM_TYPE_TEMPLATE, methodName);
            returnType = MessageFormat.format(RETURN_TYPE_TEMPLATE, returnType);

            // Format method signature
            return MessageFormat.format(METHOD_SIGNATURE_TEMPLATE, returnType, methodName, paramType);
        }
    }

    /**
     * Stores the package name.
     */
    private String namespace;

    private static final PluginContractItemDef SEND_MSG_DEF = MessagingSDKContract.buildSendMsgDef();
    private static final PluginContractItemDef RECEIVE_MSG_DEF = MessagingSDKContract.buildReceiveMsgDef();

    @Override
    public Class<Configuration> getConfigurationClass() {
        return Configuration.class;
    }

    @Override
    protected List<SdkApiFile> generatePluginSdkApi(SpecificPluginSdkApiGenerationContext<Configuration> context) {

        // Get the package name
        this.namespace = MessageFormat.format(PACKAGE_NAME_TEMPLATE, context.getConfiguration().getMessageFamilyFqn(), context.getExtensionId());

        List<SdkApiFile> sources = new ArrayList<>();
        this.signatureGenerators.put(SEND_MSG_DEF, new SendMsgMethodSignatureGenerator());
        this.signatureGenerators.put(RECEIVE_MSG_DEF, new ReceiveMsgMethodSignatureGenerator(sources));

        sources.addAll(this.generatePluginAPI(context));
        sources.addAll(this.generatePluginImplementation(context));

        return sources;
    }

    // @Override
    protected List<SdkApiFile> generatePluginAPI(SpecificPluginSdkApiGenerationContext<Configuration> context) {

        // This method might produce more than a single source file
        List<SdkApiFile> sources = new ArrayList<>();

        // A buffer for plugin API method signatures
        StringBuilder signatureBuffer = new StringBuilder();

        for (PluginContractInstance contract : context.getPluginContracts()) {
            for (PluginContractItemDef def : contract.getDef().getPluginContractItems()) {
                for (PluginContractItemInfo item : contract.getContractItemInfo(def)) {

                    // Get method name
                    String name = null;
                    try {
                        AvroByteArrayConverter<ItemConfiguration> converter = new AvroByteArrayConverter<>(ItemConfiguration.class);
                        name = converter.fromByteArray(item.getConfigurationData()).getMethodName();
                    } catch (IOException cause) {
                        // TODO: Process the exception
                    }

                    Schema.Parser parser = new Schema.Parser();

                    // Get parameter type FQN
                    String in = null;
                    if (item.getInMessageSchema() != null) {
                        in = parser.parse(item.getInMessageSchema()).getFullName();
                    }

                    // Get return type FQN
                    String out = null;
                    if (item.getOutMessageSchema() != null) {
                        out = parser.parse(item.getOutMessageSchema()).getFullName();
                    }

                    String signature = this.signatureGenerators.get(def).generateMethodSignature(name, in, out);
                    signatureBuffer.append(signature).append(";\n");
                }
            }
        }

        // Add the plugin API source file to the output
        // TODO: Extract the hardcoded constants
        String fileName = MessageFormat.format(SOURCE_FILE_NAME_TEMPLATE, MessageFormat.format(PLUGIN_API_CLASS_NAME_TEMPLATE, "Messaging"));
        byte[] fileData = this.getPluginAPIFileData("Messaging", this.namespace, signatureBuffer.toString());
        sources.add(new SdkApiFile(fileName, fileData));

        return sources;
    }

    // @Override
    protected List<SdkApiFile> generatePluginImplementation(SpecificPluginSdkApiGenerationContext<Configuration> context) {
        // TODO: Needs implementation
        return Collections.emptyList();
    }

    /**
     * Generates the body of a plugin API source file.
     */
    private byte[] getPluginAPIFileData(String className, String packageName, String methodSignatures) {
        String content = this.readFileAsString(PLUGIN_API_TEMPLATE_FILE);
        content = content.replace(PACKAGE_NAME, packageName);
        content = content.replace(CLASS_NAME, MessageFormat.format(PLUGIN_API_CLASS_NAME_TEMPLATE, className));
        content = content.replace(METHOD_SIGNATURES, methodSignatures);
        return content.getBytes();
    }

    /**
     * Generates the body of a plugin API implementation source file.
     */
    private byte[] getPluginImplementationFileData(String className, String packageName, Object... objects) {
        String content = this.readFileAsString(PLUGIN_IMPLEMENTATION_TEMPLATE_FILE);
        content = content.replace(PACKAGE_NAME, packageName);
        content = content.replace(CLASS_NAME, MessageFormat.format(PLUGIN_IMPLEMENTATION_CLASS_NAME_TEMPLATE, className));
        // TODO: Finish the implementation
        return content.getBytes();
    }

    // TODO: Used for testing purposes, remove when unnecessary
    public static void main(String[] args) throws IOException {
        EndpointMessagePluginGenerator subject = new EndpointMessagePluginGenerator();
        SpecificPluginSdkApiGenerationContext<Configuration> context = EndpointMessagePluginGenerator.getHardcodedContext();
        List<SdkApiFile> sources = subject.generatePluginSdkApi(context);
        for (SdkApiFile source : sources) {
            System.out.println(source.getFileName());
            System.out.println(new String(source.getFileData()));
        }
    }

    private static SpecificPluginSdkApiGenerationContext<Configuration> getHardcodedContext() throws IOException {
        PluginContractDef def = MessagingSDKContract.buildMessagingSDKContract();
        final BasePluginContractInstance instance = new BasePluginContractInstance(def);

        GenericAvroConverter<ItemConfiguration> methodNameConverter = new GenericAvroConverter<ItemConfiguration>(ItemConfiguration.SCHEMA$);

        PluginContractItemDef sendMsgDef = MessagingSDKContract.buildSendMsgDef();
        PluginContractItemDef receiveMsgDef = MessagingSDKContract.buildReceiveMsgDef();

        // Future<Void> sendA(ClassA msg);
        PluginContractItemInfo info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("sendA")))
                .withInMsgSchema(ClassA.SCHEMA$.toString()).build();
        instance.addContractItemInfo(sendMsgDef, info);

        // Future<ClassA> getA()
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("getA")))
                .withOutMsgSchema(ClassA.SCHEMA$.toString()).build();
        instance.addContractItemInfo(sendMsgDef, info);

        // Future<ClassB> getB(ClassA msg);
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("getB")))
                .withInMsgSchema(ClassA.SCHEMA$.toString()).withOutMsgSchema(ClassB.SCHEMA$.toString()).build();
        instance.addContractItemInfo(sendMsgDef, info);

        // Future<ClassC> getC(ClassA msg);
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("getC")))
                .withInMsgSchema(ClassA.SCHEMA$.toString()).withOutMsgSchema(ClassC.SCHEMA$.toString()).build();
        instance.addContractItemInfo(sendMsgDef, info);

        // void setMethodAListener(MethodAListener listener);
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("setMethodAListener")))
                .withInMsgSchema(ClassC.SCHEMA$.toString()).withOutMsgSchema(ClassA.SCHEMA$.toString()).build();
        instance.addContractItemInfo(receiveMsgDef, info);

        // void setMethodBListener(MethodBListener listener);
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("setMethodBListener")))
                .withInMsgSchema(ClassC.SCHEMA$.toString()).withOutMsgSchema(ClassB.SCHEMA$.toString()).build();
        instance.addContractItemInfo(receiveMsgDef, info);

        // void setMethodCListener(MethodCListener listener);
        info = BasePluginContractItemInfo.builder().withData(methodNameConverter.encode(new ItemConfiguration("setMethodCListener")))
                .withOutMsgSchema(ClassC.SCHEMA$.toString()).build();
        instance.addContractItemInfo(receiveMsgDef, info);

        PluginSdkApiGenerationContext base = new PluginSdkApiGenerationContext() {

            @Override
            public Set<PluginContractInstance> getPluginContracts() {
                return Collections.<PluginContractInstance> singleton(instance);
            }

            @Override
            public byte[] getPluginConfigurationData() {
                // Not used here
                return null;
            }

            @Override
            public int getExtensionId() {
                // This is used in order to put all auto-generated code into
                // "org.kaaproject.kaa.client.plugin.messaging.ext1" folder
                return 1;
            }
        };
        Configuration configuration = new Configuration("org.kaaproject.kaa.client.plugin.messaging");
        return new SpecificPluginSdkApiGenerationContext<Configuration>(base, configuration);
    }
}
