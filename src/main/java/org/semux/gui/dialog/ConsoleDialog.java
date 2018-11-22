/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.semux.api.v2.SemuxApi;
import org.semux.api.v2.SemuxApiImpl;
import org.semux.gui.SemuxGui;
import org.semux.message.GuiMessages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.swagger.annotations.ApiOperation;
import org.semux.util.CircularFixedSizeList;

public class ConsoleDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String HELP = "help";
    private static final String NULL = "null";

    private final JTextArea console;
    private final JTextField input;

    private final transient SemuxApiImpl api;
    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient Map<String, MethodDescriptor> methods = new TreeMap<>();
    private final transient CircularFixedSizeList<String> commandHistory = new CircularFixedSizeList<>(10);

    public ConsoleDialog(SemuxGui gui, JFrame parent) {

        super(null, GuiMessages.get("Console"), ModalityType.MODELESS);

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


        // add history for cycling through past commands
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(
                "UP"), "historyBack");
        input.getActionMap().put("historyBack", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = commandHistory.back();
                if(command!=null) {
                    input.setText(command);
                }
            }
        });
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(
                "DOWN"), "historyForward");
        input.getActionMap().put("historyForward", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = commandHistory.forward();
                if(command!=null) {
                    input.setText(command);
                }
            }
        });

        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(input, BorderLayout.SOUTH);

        this.setSize(800, 600);
        this.setLocationRelativeTo(parent);

        this.api = new org.semux.api.v2.SemuxApiImpl(gui.getKernel());
        for (Method m : SemuxApi.class.getMethods()) {
            MethodDescriptor md = parseMethod(m);
            this.methods.put(md.name, md);
        }

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
        commandHistory.add(command);

        console.append("\n");
        console.append("> " + command);
        console.append("\n");

        switch (command) {
        case HELP:
            printHelp();
            break;
        default:
            console.append("\n");
            console.append(callApi(command));
            console.append("\n");
            break;

        }
        input.setText("");
        console.setCaretPosition(console.getDocument().getLength());
    }

    /**
     * Use reflection to call methods
     *
     * @param input
     */
    protected String callApi(String input) {
        String[] commandArguments = input.split(" ");
        String command = commandArguments[0];

        MethodDescriptor md = methods.get(command);
        if (md == null) {
            return GuiMessages.get("UnknownMethod", command);
        }

        try {
            Method method = api.getClass().getMethod(command, md.argumentTypes);
            Object[] arguments = new Object[md.argumentTypes.length];

            if (arguments.length < commandArguments.length - 1) {
                return GuiMessages.get("MethodError", command);
            }

            for (int i = 0; i < commandArguments.length - 1; i++) {
                String argument = commandArguments[i + 1];
                if (md.argumentTypes[i] == Boolean.class) {
                    arguments[i] = Boolean.parseBoolean(argument);
                } else {
                    if (NULL.equals(argument)) {
                        arguments[i] = null;
                    } else {
                        arguments[i] = commandArguments[i + 1];
                    }
                }
            }

            Response response = (Response) method.invoke(api, arguments);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(response.getEntity());
        } catch (NoSuchMethodException e) {
            return GuiMessages.get("UnknownMethod", command);
        } catch (InvocationTargetException | IllegalAccessException | JsonProcessingException e) {
            return GuiMessages.get("MethodError", command);
        }
    }

    private void printHelp() {
        for (MethodDescriptor md : methods.values()) {
            console.append(md.description);
        }

        console.append("\n");
    }

    private MethodDescriptor parseMethod(Method method) {
        String name = method.getName();
        List<Class<?>> argumentTypes = new ArrayList<>();

        // get the annotation
        Path path = method.getAnnotation(Path.class);
        if (path == null) {
            // not a web method
            return null;
        }

        StringBuilder builder = new StringBuilder();

        ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);

        if (apiOperation != null) {
            String description = apiOperation.value();
            if (!description.trim().isEmpty()) {
                builder.append(description).append("\n\t");
            }
        }

        builder.append(method.getName());
        for (Parameter parameter : method.getParameters()) {
            argumentTypes.add(parameter.getType());

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

        return new MethodDescriptor(name, argumentTypes.toArray(new Class<?>[0]),
                builder.toString());
    }

    private static class MethodDescriptor {
        private String name;
        private Class<?>[] argumentTypes;
        private String description;

        public MethodDescriptor(String name, Class<?>[] argumentTypes, String description) {
            this.name = name;
            this.argumentTypes = argumentTypes;
            this.description = description;
        }
    }
}
