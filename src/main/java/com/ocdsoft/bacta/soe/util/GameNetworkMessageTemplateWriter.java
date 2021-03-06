package com.ocdsoft.bacta.soe.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ocdsoft.bacta.soe.ServerState;
import com.ocdsoft.bacta.soe.ServerType;
import com.ocdsoft.bacta.soe.io.udp.NetworkConfiguration;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

/**
 * Created by kburkhardt on 1/31/15.
 */
@Singleton
public final class GameNetworkMessageTemplateWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameNetworkMessageTemplateWriter.class);

    private final VelocityEngine ve;
    private final ServerType serverEnv;

    private final String controllerClassPath;
    private final String controllerFilePath;

    private final String messageFilePath;
    private final String messageClassPath;

    private final String objControllerClassPath;
    private final String objControllerFilePath;

    private final String objMessageFilePath;
    private final String objMessageClassPath;

    private final String commandControllerClassPath;
    private final String commandControllerFilePath;

    private final String commandMessageFilePath;
    private final String commandMessageClassPath;

    private final String tangibleClassPath;


    @Inject
    public GameNetworkMessageTemplateWriter(final NetworkConfiguration configuration, final ServerState serverState) {

        this.serverEnv = serverState.getServerType();

        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, LOGGER);
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

        ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
        ve.init();

        controllerClassPath = configuration.getBasePackage() + ".controller." + serverEnv.name().toLowerCase();
        String fs = System.getProperty("file.separator");

        controllerFilePath = System.getProperty("user.dir") + fs + System.getProperty("bacta.serverPath") + "src"
                + fs + "main" + fs + "java" + fs +
                configuration.getBasePackage().replace(".", fs) + fs + "controller" + fs +
                serverEnv.name().toLowerCase() + fs;
        
        messageClassPath = configuration.getBasePackage() + ".message." + serverEnv.name().toLowerCase();
        messageFilePath = System.getProperty("user.dir") + fs + System.getProperty("bacta.serverPath") + "src"
                + fs + "main" + fs + "java" + fs +
                configuration.getBasePackage().replace(".", fs) + fs + "message" + fs +
                serverEnv.name().toLowerCase() + fs;

        tangibleClassPath = configuration.getBasePackage() + ".object.tangible.TangibleObject";

        objControllerClassPath = controllerClassPath  + ".object";
        objControllerFilePath = controllerFilePath + fs + "object" + fs;

        objMessageClassPath = messageClassPath + ".object";
        objMessageFilePath = messageFilePath + fs + "object" + fs;

        commandControllerClassPath = objControllerClassPath + ".command";
        commandControllerFilePath = objControllerFilePath + fs + "command" + fs;

        commandMessageClassPath = objMessageClassPath + ".command";
        commandMessageFilePath = objMessageFilePath + fs + "command" + fs;
    }

    public void createGameNetworkMessageFiles(short priority, int opcode, ByteBuffer buffer) {

        String messageName = ClientString.get(opcode);
        
        if (messageName.isEmpty() || messageName.equalsIgnoreCase("unknown")) {
            LOGGER.error("Unknown message opcode: 0x" + Integer.toHexString(opcode));
            return;
        }

        writeGameNetworkMessage(opcode, priority, messageName, buffer);
        writeGameNetworkController(messageName);
    }

    private void writeGameNetworkMessage(int opcode, short priority, String messageName, ByteBuffer buffer)  {

        String outFileName = messageFilePath + messageName + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'{}' already exists", messageName);
            return;
        }

        Template template = ve.getTemplate("/template/GameNetworkMessage.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", messageClassPath);
        context.put("messageName", messageName);

        if(SOECRC32.hashCode(messageName) == opcode) {
            context.put("messageSimpleName", "SOECRC32.hashCode(" + messageName + ".class.getSimpleName())");
        } else {
            context.put("messageSimpleName", "0x" + Integer.toHexString(opcode));
        }

        String messageStruct = SoeMessageUtil.makeMessageStruct(buffer);
        context.put("messageStruct", messageStruct);

        context.put("priority", "0x" + Integer.toHexString(priority));
        context.put("opcode", "0x" + Integer.toHexString(opcode));

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }
    
    private void writeGameNetworkController(String messageName) {

        String className = messageName + "Controller";
        
        String outFileName = controllerFilePath + className + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'" + className + "' already exists");
            return;
        }

        Template template = ve.getTemplate("/template/GameNetworkController.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", controllerClassPath);
        context.put("messageClasspath", messageClassPath);
        context.put("serverType", "ServerType." + serverEnv);
        context.put("messageName", messageName);
        context.put("messageNameClass", messageName + ".class");
        context.put("className", className);

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }

    public void createObjFiles(int opcode, ByteBuffer buffer) {

        String messageName = ObjectControllerNames.get(opcode);

        if (messageName.isEmpty() || messageName.equalsIgnoreCase("unknown")) {
            LOGGER.error("Unknown message opcode: 0x" + Integer.toHexString(opcode));
            return;
        }

        writeObjMessage(opcode, messageName, buffer, messageName);
        writeObjController(opcode, messageName);
    }

    private void writeObjMessage(int opcode, String messageName, ByteBuffer buffer, String objectControllerName)  {

        String outFileName = objMessageFilePath + messageName + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'" + messageName + "' already exists");
            return;
        }

        Template template = ve.getTemplate("/template/ObjMessage.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", objMessageClassPath);
        context.put("objectControllerName", objectControllerName);
        context.put("messageName", messageName);
        context.put("controllerid", Integer.toHexString(opcode));

        String messageStruct = SoeMessageUtil.makeMessageStruct(buffer);
        context.put("messageStruct", messageStruct);

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }

    private void writeObjController(int opcode, String messageName) {

        String className = messageName + "ObjController";
        String outFileName = objControllerFilePath + className + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'" + className + "' already exists");
            return;
        }

        Template template = ve.getTemplate("/template/ObjController.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", objControllerClassPath);
        context.put("tangibleClassPath", tangibleClassPath);
        context.put("messageClasspath", objMessageClassPath);
        context.put("messageName", messageName);
        context.put("className", className);
        context.put("controllerid", opcode);

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }

    public void createCommandFiles(int commandHash, ByteBuffer buffer) {

        String messageName = CommandNames.get(commandHash);

        if (messageName.isEmpty() || messageName.equalsIgnoreCase("unknown")) {
            LOGGER.error("Unknown message opcode: 0x" + Integer.toHexString(commandHash));
            return;
        }

        messageName = messageName.substring(0, 1).toUpperCase() + messageName.substring(1);

        writeCommandMessage(commandHash, messageName, buffer);
        writeCommandController(commandHash, messageName);
    }

    private void writeCommandMessage(int opcode, String messageName, ByteBuffer buffer)  {

        String outFileName = commandMessageFilePath + messageName + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'" + messageName + "' already exists");
            return;
        }

        Template template = ve.getTemplate("/template/CommandMessage.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", commandMessageClassPath);
        //context.put("objectControllerName", objectControllerName);
        context.put("messageName", messageName);
        context.put("controllerid", Integer.toHexString(opcode));

        String messageStruct = SoeMessageUtil.makeMessageStruct(buffer);
        context.put("messageStruct", messageStruct);

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }

    private void writeCommandController(int opcode, String messageName) {

        String className = messageName + "CommandController";

        String outFileName = commandControllerFilePath + className + ".java";
        File file = new File(outFileName);
        if(file.exists()) {
            LOGGER.info("'" + className + "' already exists");
            return;
        }

        Template template = ve.getTemplate("/template/CommandController.vm");

        VelocityContext context = new VelocityContext();

        context.put("packageName", commandControllerClassPath);
        context.put("tangibleClassPath", tangibleClassPath);
        context.put("messageClasspath", commandMessageClassPath + "." + messageName);
        context.put("messageName", messageName);
        context.put("className", className);
        context.put("controllerid", opcode);

        /* lets render a template */
        writeTemplate(outFileName, context, template);
    }

    private void writeTemplate(String outFileName, VelocityContext context, Template template) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFileName)));

            if (!ve.evaluate(context, writer, template.getName(), "")) {
                throw new Exception("Failed to convert the template into class.");
            }

            template.merge(context, writer);

            writer.flush();
            writer.close();
        } catch(Exception e) {
            LOGGER.error("Unable to write message", e);
        }
    }

}
