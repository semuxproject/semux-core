/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.semux.api.v1_0_2.client.SemuxApi;
import org.semux.api.v1_0_2.impl.SemuxApiServiceImpl;
import org.semux.gui.SemuxGui;
import org.semux.message.GuiMessages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.swagger.annotations.ApiOperation;

public class ConsoleDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    public static final String HELP = "help";

    private transient SemuxApiServiceImpl api;

    private JTextArea console;
    private JTextField input;
    private ObjectMapper mapper = new ObjectMapper();

    public ConsoleDialog(SemuxGui gui, JFrame parent) {

        super(null, GuiMessages.get("Console"), Dialog.ModalityType.MODELESS);

        setName("Console");

        console = new JTextArea();
        console.setEditable(false);
        console.setName("txtConsole");
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(console);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        input = new JTextField();
        input.addActionListener(this);
        input.setName("txtInput");

        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(input, BorderLayout.SOUTH);

        this.setSize(800, 600);
        this.setLocationRelativeTo(parent);
        api = new SemuxApiServiceImpl(gui.getKernel());

        console.append(GuiMessages.get("ConsoleHelp", HELP));
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                input.requestFocus();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = input.getText();

        console.append("\n");
        console.append("> " + command);
        console.append("\n");

        switch (command) {
        case HELP:
            printHelp();
            break;
        default:
            callApi(command);
            break;

        }
        input.setText("");
        console.setCaretPosition(console.getDocument().getLength());
    }

    /**
     * Use reflection to call methods
     *
     * @param commandString
     */
    private void callApi(String commandString) {
        String[] commandParams = commandString.split(" ");

        String command = commandParams[0];

        // console only supports string parameters;
        int numParams = commandParams.length - 1;
        Class<?>[] classes = new Class[numParams];
        for (int i = 0; i < numParams; i++) {
            classes[i] = String.class;
        }

        try {
            Method method = api.getClass().getMethod(command, classes);
            Object[] params = Arrays.copyOfRange(commandParams, 1, commandParams.length);
            Response response = (Response) method.invoke(api, params);

            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            console.append("\n");
            console.append(mapper.writeValueAsString(response.getEntity()));
            console.append("\n");
        } catch (NoSuchMethodException e) {
            console.append(GuiMessages.get("UnknownMethod", command));
        } catch (InvocationTargetException | IllegalAccessException | JsonProcessingException e) {
            console.append(GuiMessages.get("MethodError", command));
        }
    }

    private void printHelp() {
        Method[] apiMethods = SemuxApi.class.getMethods();
        for (Method method : apiMethods) {
            String methodString = getMethodString(method);

            if (methodString != null) {
                console.append(methodString);
            }
        }

        console.append("\n");
    }

    private String getMethodString(Method method) {
        // get the annotation
        Path path = method.getAnnotation(Path.class);
        if (path == null) {
            // not a web method
            return null;
        }
        StringBuilder builder = new StringBuilder();

        builder.append(method.getName());
        for (Parameter parameter : method.getParameters()) {

            if (!parameter.getType().equals(String.class)) {
                // we only currently support string types
                return null;
            }

            builder.append(" ");
            QueryParam param = parameter.getAnnotation(QueryParam.class);
            builder.append("<");
            if (param != null) {
                builder.append(param.value());
            } else {
                builder.append(parameter.getName());
            }
            builder.append(">");
        }

        ApiOperation operation = method.getAnnotation(ApiOperation.class);
        if (operation != null) {
            builder.append("\n\t").append(operation.notes()).append("\n");
        }

        builder.append("\n");
        return builder.toString();
    }
}
