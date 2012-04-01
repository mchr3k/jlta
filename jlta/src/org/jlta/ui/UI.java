package org.jlta.ui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Map;

import net.miginfocom.swt.MigLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jlta.common.ThreadData;
import org.jlta.common.ThreadData.ThreadState;

public class UI
{
  private final Shell window;

  private final Group connectGroup;
  private final Label hostLabel;
  private final Text hostText;
  private final Label portLabel;
  private final Text portText;
  private final Button connectButton;

  private final Group actionsGroup;
  private final Button fetchButton;
  private final Button resetButton;

  private final Group outputGroup;
  private final Text outputText;

  private State state = State.DISCONNECTED;
  private Socket socket = null;
  private ObjectInputStream dataIn = null;
  private ObjectOutputStream dataOut = null;

  public enum State
  {
    DISCONNECTED,
    CONNECTED;
  }

  public UI(Shell xiWindow)
  {
    window = xiWindow;
    MigLayout mainLayout = new MigLayout("fill",
                                         "[][][grow]", // Columns
                                         "[]0[grow]");  // Rows
    window.setLayout(mainLayout);

    connectGroup = new Group(window, SWT.NONE);
    connectGroup.setText("Connection");
    MigLayout connectLayout = new MigLayout("fill",
                                            "[][150][100]", // Columns
                                            "[][]");  // Rows
    connectGroup.setLayout(connectLayout);
    hostLabel = new Label(connectGroup, SWT.NONE);
    hostLabel.setText("Host: ");
    hostText = new Text(connectGroup, SWT.BORDER);
    hostText.setLayoutData("growx");
    hostText.setText("localhost");
    connectButton = new Button(connectGroup, SWT.NONE);
    connectButton.setLayoutData("spany 2,wrap,grow");
    connectButton.setText("Connect");
    portLabel = new Label(connectGroup, SWT.NONE);
    portLabel.setText("Port: ");
    portText = new Text(connectGroup, SWT.BORDER);
    portText.setLayoutData("growx");
    connectGroup.setTabList(new Control[] {hostText, portText, connectButton});

    connectButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent arg0)
      {
        if (state == State.DISCONNECTED)
        {
          connectButton.setText("Connecting...");
          connectButton.setEnabled(false);

          final String host = hostText.getText();
          final String port = portText.getText();

          Runnable r = new Runnable()
          {
            @Override
            public void run()
            {
              connect(host,port);
            }
          };
          Thread t = new Thread(r);
          t.setName("Connection Thread");
          t.setDaemon(true);
          t.start();
        }
        else
        {
          disconnect();
        }
      }
    });

    actionsGroup = new Group(window, SWT.NONE);
    actionsGroup.setText("Actions");
    MigLayout actionLayout = new MigLayout("fill",
                                           "[][]", // Columns
                                           "[]");  // Rows
    actionsGroup.setLayout(actionLayout);
    fetchButton = new Button(actionsGroup, SWT.NONE);
    fetchButton.setText("Fetch Data");
    fetchButton.setLayoutData("growy");
    resetButton = new Button(actionsGroup, SWT.NONE);
    resetButton.setText("Reset Data on Agent");
    resetButton.setLayoutData("growy");
    actionsGroup.setLayoutData("grow,wrap");

    fetchButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent arg0)
      {
        Runnable r = new Runnable()
        {
          @Override
          public void run()
          {
            fetchData();
          }
        };
        Thread t = new Thread(r);
        t.setName("Fetch Data");
        t.setDaemon(true);
        t.start();
      }
    });

    resetButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent arg0)
      {
        Runnable r = new Runnable()
        {
          @Override
          public void run()
          {
            resetData();
          }
        };
        Thread t = new Thread(r);
        t.setName("Reset Thread");
        t.setDaemon(true);
        t.start();
      }
    });

    outputGroup = new Group(window, SWT.NONE);
    outputGroup.setText("Output");
    MigLayout outputLayout = new MigLayout("fill",
                                           "[grow]",  // Columns
                                           "[grow]"); // Rows
    outputGroup.setLayout(outputLayout);
    outputGroup.setLayoutData("grow,spanx 3,hmin 0,wmin 0");
    outputText = new Text(outputGroup, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    outputText.setLayoutData("grow,hmin 0,wmin 0");
    outputText.setEditable(false);
    outputText.addKeyListener(new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        if(((e.stateMask & SWT.CTRL) == SWT.CTRL) && (e.keyCode == 'a'))
        {
          outputText.selectAll();
        }
      }
    });

    portText.forceFocus();
  }

  private void connect(String host, String portstr)
  {
    try
    {
      int port = Integer.parseInt(portstr);
      socket = new Socket(host, port);
      dataOut = new ObjectOutputStream(socket.getOutputStream());
      dataIn = new ObjectInputStream(socket.getInputStream());
      window.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          connectButton.setText("Disconnect");
          connectButton.setEnabled(true);
        }
      });
      state = State.CONNECTED;
    }
    catch (Exception e)
    {
      disconnect();
      error(e);
    }
  }

  private void disconnect()
  {
    state = State.DISCONNECTED;
    socket = null;
    dataIn = null;
    dataOut = null;
    window.getDisplay().syncExec(new Runnable()
    {
      @Override
      public void run()
      {
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void fetchData()
  {
    try
    {
      dataOut.writeObject("fetch");
      dataOut.flush();

      final Map<Integer, ThreadData> data = (Map<Integer, ThreadData>)dataIn.readObject();
      window.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          StringBuilder str = new StringBuilder();
          str.append(new Date().toString() + " >> Collected Thread Data:\n");
          str.append("\n");
          if (data.size() == 0)
          {
            str.append("  No data\n");
          }
          else
          {
            for (ThreadData tdata : data.values())
            {
              str.append(tdata.name + " : " + tdata.state + "\n");
              if (tdata.state == ThreadState.FINISHED)
              {
                str.append(" >> Runtime: " + tdata.elapsed + " ms\n");
              }
              for (StackTraceElement stackline : tdata.newThreadStack)
              {
                str.append(" > " + stackline.toString() + "\n");
              }
              str.append("\n");
            }
          }

          outputText.setText(str.toString());
        }
      });
    }
    catch (Exception e)
    {
      disconnect();
      error(e);
    }
  }

  private void resetData()
  {
    try
    {
      dataOut.writeObject("reset");
      dataOut.flush();
      window.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          outputText.setText(new Date().toString() + " - Data Reset!");
        }
      });
    }
    catch (IOException e)
    {
      disconnect();
      error(e);
    }
  }

  private void error(final Exception e)
  {
    window.getDisplay().syncExec(new Runnable()
    {
      @Override
      public void run()
      {
        MessageBox messageBox = new MessageBox(window, SWT.ICON_ERROR | SWT.OK);
        messageBox.setMessage("Error: " + e.toString());
        messageBox.open();
      }
    });
  }

  public void open()
  {
    window.open();
    Display display = Display.getDefault();
    while (!window.isDisposed())
    {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  public static void main(String[] args)
  {
    final Shell window = new Shell();
    window.setSize(new Point(650, 600));
    window.setMinimumSize(new Point(650, 600));
    window.setText("Java Live Thread Analyser");

    // Fill in UI
    UI ui = new UI(window);

    // Open UI
    ui.open();
  }
}
