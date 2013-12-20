package org.jlta.ui;

import net.miginfocom.swt.MigLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jlta.common.ServerCommunication;
import org.jlta.common.StackTraceArrayWrap;
import org.jlta.common.ThreadData;
import org.jlta.common.ThreadData.ThreadState;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static org.jlta.common.ServerCommunication.State;

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

  private final Group filtersGroup;
  private final Label threadStatesLabel;
  private final Button allocatedButton;
  private final Button startedButton;
  private final Button finishedButton;
  private final Label separatorLabel1;
  private final Button ignoreNamedButton;
  private final Combo contextsCombo;
  private final Label contextsLabel;
  private final Label separatorLabel2;
  private final Button stackLimitButton;
  private final Spinner stackLimitSpinner;

  private final Group outputGroup;
  private final Text outputText;

  private Map<Integer, ThreadData> data = new HashMap<Integer, ThreadData>();

  private ServerCommunication server = new ServerCommunication();


  public UI(Shell xiWindow)
  {
    window = xiWindow;
    MigLayout mainLayout = new MigLayout("fill",
                                         "[][][grow]", // Columns
                                         "[]0[]0[grow]");  // Rows
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
        if (server.getState() == State.DISCONNECTED)
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
        fetchButton.setEnabled(false);
        resetButton.setEnabled(false);
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

    filtersGroup = new Group(window, SWT.NONE);
    filtersGroup.setText("Filters");
    MigLayout filtersLayout = new MigLayout("fill",
                                            "[][][][][][][][][][][][grow]",  // Columns
                                            "[]"); // Rows
    filtersGroup.setLayout(filtersLayout);
    filtersGroup.setLayoutData("grow,spanx 3,wrap");
    SelectionAdapter applyFilter = new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent arg0)
      {
        uiProcessData();
      }
    };
    threadStatesLabel = new Label(filtersGroup, SWT.NONE);
    threadStatesLabel.setText("States: ");
    allocatedButton = new Button(filtersGroup, SWT.CHECK);
    allocatedButton.setText("Allocated");
    allocatedButton.addSelectionListener(applyFilter);
    allocatedButton.setSelection(true);
    startedButton = new Button(filtersGroup, SWT.CHECK);
    startedButton.setText("Started");
    startedButton.addSelectionListener(applyFilter);
    startedButton.setSelection(true);
    finishedButton = new Button(filtersGroup, SWT.CHECK);
    finishedButton.setText("Finished");
    finishedButton.addSelectionListener(applyFilter);
    finishedButton.setSelection(true);
    separatorLabel1 = new Label(filtersGroup, SWT.NONE);
    separatorLabel1.setText("|");
    ignoreNamedButton = new Button(filtersGroup, SWT.CHECK);
    ignoreNamedButton.setText("Ignore Named Threads");
    ignoreNamedButton.addSelectionListener(applyFilter);

    contextsLabel = new Label(filtersGroup, SWT.NONE);
    contextsLabel.setText("| Contexts: ");
    contextsCombo = new Combo(filtersGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
    contextsCombo.setItems(new String[] {"All"});
    contextsCombo.select(0);
    contextsCombo.addSelectionListener(applyFilter);
    contextsCombo.setLayoutData("width 80px,wmax 80px");
    separatorLabel2 = new Label(filtersGroup, SWT.NONE);
    separatorLabel2.setText("|");
    stackLimitButton = new Button(filtersGroup, SWT.CHECK);
    stackLimitButton.setText("Limit stack");
    stackLimitButton.addSelectionListener(applyFilter);
    stackLimitSpinner = new Spinner(filtersGroup, SWT.NONE);
    stackLimitSpinner.setMinimum(1);
    stackLimitSpinner.setIncrement(1);
    stackLimitSpinner.setMaximum(100);
    stackLimitSpinner.setSelection(1);
    stackLimitSpinner.addSelectionListener(applyFilter);

    outputGroup = new Group(window, SWT.NONE);
    outputGroup.setText("Output");
    MigLayout outputLayout = new MigLayout("fill",
                                           "[grow]",  // Columns
                                           "[grow]"); // Rows
    outputGroup.setLayout(outputLayout);
    outputGroup.setLayoutData("grow,spanx 3,hmin 0,wmin 0");
    outputText = new Text(outputGroup, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    outputText.setLayoutData("grow,hmin 0,wmin 0");
    Color textBackground = outputText.getBackground();
    outputText.setEditable(false);
    // Reset background to color used before we set editable false
    outputText.setBackground(textBackground);
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

    disconnect();
    portText.forceFocus();
  }

  private void connect(String host, String portstr)
  {
    try
    {
      int port = Integer.parseInt(portstr);
      server.connect(host, port);
      window.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          connectButton.setText("Disconnect");
          connectButton.setEnabled(true);
          fetchButton.setEnabled(true);
          resetButton.setEnabled(true);
          allocatedButton.setEnabled(true);
          startedButton.setEnabled(true);
          finishedButton.setEnabled(true);
          ignoreNamedButton.setEnabled(true);
          contextsCombo.setEnabled(true);
          stackLimitButton.setEnabled(true);
          stackLimitSpinner.setEnabled(true);
          fetchButton.forceFocus();
        }
      });
    }
    catch (Exception e)
    {
      disconnect();
      error(e);
    }
  }

  private void disconnect()
  {
    server.disconnect();
    window.getDisplay().syncExec(new Runnable()
    {
      @Override
      public void run()
      {
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
        fetchButton.setEnabled(false);
        resetButton.setEnabled(false);
        allocatedButton.setEnabled(false);
        startedButton.setEnabled(false);
        finishedButton.setEnabled(false);
        ignoreNamedButton.setEnabled(false);
        contextsCombo.setEnabled(false);
        stackLimitButton.setEnabled(false);
        stackLimitSpinner.setEnabled(false);
      }
    });
  }

  private void fetchData()
  {
    try
    {
      server.fetch();
      window.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          uiProcessData();
        }
      });
    }
    catch (Exception e)
    {
      disconnect();
      error(e);
    }
  }


  private void uiProcessData()
  {
    final boolean allocated = allocatedButton.getSelection();
    final boolean started = startedButton.getSelection();
    final boolean finished = finishedButton.getSelection();
    final boolean ignorenamed = ignoreNamedButton.getSelection();
    final String context = contextsCombo.getItem(contextsCombo.getSelectionIndex());
    final boolean stacklimit = stackLimitButton.getSelection();
    final int stacklimitval = stackLimitSpinner.getSelection();
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        processData(allocated, started, finished,
                    ignorenamed, context,
                    stacklimit, stacklimitval);
      }
    };
    Thread t = new Thread(r);
    t.setName("Display Data");
    t.setDaemon(true);
    t.start();
  }

  private final Pattern unnamedThread = Pattern.compile("Thread-[\\d]+");
  private final Pattern unnamedTimer = Pattern.compile("Timer-[\\d]+");

  private void processData(boolean allocated,
                           boolean started,
                           boolean finished,
                           boolean ignorenamed,
                           final String context,
                           boolean stacklimit,
                           int stacklimitval)
  {
    final Set<String> contextsSet = new HashSet<String>();
    final Map<StackTraceArrayWrap, List<ThreadData>> groupedData = new HashMap<StackTraceArrayWrap, List<ThreadData>>();
    int allThreadCount = 0;
    int filteredThreadCount = 0;
    for (ThreadData tdata : server.getData().values())
    {
      allThreadCount++;
      if ((tdata.context != null) &&
          (tdata.context.trim().length() > 0))
      {
        contextsSet.add(tdata.context.trim());
      }

      if ((tdata.state == ThreadState.ALLOCATED) && !allocated)
        continue;
      if ((tdata.state == ThreadState.STARTED) && !started)
        continue;
      if ((tdata.state == ThreadState.FINISHED) && !finished)
        continue;
      if (ignorenamed &&
          (tdata.name != null) &&
          !unnamedThread.matcher(tdata.name).matches() &&
          !unnamedTimer.matcher(tdata.name).matches())
      {
        continue;
      }
      if (!"All".equals(context) &&
          !context.equals(tdata.context))
      {
        continue;
      }

      StackTraceElement[] tstack = tdata.newThreadStack;
      if (stacklimit &&
          (tstack.length > stacklimitval))
      {
        tstack = Arrays.copyOfRange(tstack, 0, stacklimitval);
      }

      StackTraceArrayWrap stackWrap = new StackTraceArrayWrap(tstack);
      List<ThreadData> threadData = groupedData.get(stackWrap);
      if (threadData == null)
      {
        threadData = new ArrayList<ThreadData>();
        groupedData.put(stackWrap, threadData);
      }
      threadData.add(tdata);
      filteredThreadCount++;
    }

    final Map<StackTraceArrayWrap, List<ThreadData>> sortedGroupedData = new TreeMap<StackTraceArrayWrap, List<ThreadData>>(new Comparator<StackTraceArrayWrap>()
    {
      @Override
      public int compare(StackTraceArrayWrap o1, StackTraceArrayWrap o2)
      {
        int o1Count = groupedData.get(o1).size();
        int o2Count = groupedData.get(o2).size();
        if (o1Count < o2Count)
        {
          return 1;
        }
        else if (o1Count > o2Count)
        {
          return -1;
        }
        else
        {
          if (groupedData.get(o1).hashCode() < groupedData.get(o2).hashCode())
          {
            return 1;
          }
          else
          {
            return -1;
          }
        }
      }
    });
    sortedGroupedData.putAll(groupedData);

    final List<String> contextsList = new ArrayList<String>(contextsSet);
    Collections.sort(contextsList);
    contextsList.add(0, "All");

    final String[] contextsArray = contextsList.toArray(new String[contextsList.size()]);

    final int finalAllThreadCount = allThreadCount;
    final int finalFilteredThreadCount = filteredThreadCount;
    window.getDisplay().syncExec(new Runnable()
    {
      @Override
      public void run()
      {
        contextsCombo.setItems(contextsArray);
        int selection = 0;
        for (int ii = 0; ii < contextsArray.length; ii++)
        {
          if (contextsArray[ii].equals(context))
          {
            selection = ii;
            break;
          }
        }
        contextsCombo.select(selection);

        StringBuilder str = new StringBuilder();

        str.append(new Date().toString() + " >> Thread Data:\n");
        str.append("\n");
        str.append("Total threads: " + finalAllThreadCount + "\n");
        str.append("Displayed threads: " + finalFilteredThreadCount + "\n");
        str.append("\n");
        if (groupedData.size() > 0)
        {
          for (Entry<StackTraceArrayWrap, List<ThreadData>> entry : sortedGroupedData.entrySet())
          {
            StackTraceElement[] stack = entry.getKey().stack;
            List<ThreadData> threads = entry.getValue();
            Collections.sort(threads);
            str.append(threads.size() + " threads:\n");
            for (ThreadData tdata : threads)
            {
              str.append(" > " + tdata.name + " : " + tdata.state);
                if(tdata.state == ThreadState.STARTED || tdata.state == ThreadState.FINISHED) {
                    str.append(" (Started: " + new Date(tdata.startTime)+ ")");
                }
              if (tdata.state == ThreadState.FINISHED)
              {
                str.append(" (Runtime: " + tdata.elapsed + " ms)");
              }
              if ((tdata.context != null) && (tdata.context.length() > 0))
              {
                str.append(" (" + tdata.context + ")");
              }
              str.append("\n");
            }
            str.append("Stack:\n");
            for (StackTraceElement stackline : stack)
            {
              str.append(" > " + stackline.toString() + "\n");
            }
            str.append("\n");
          }
        }

        outputText.setText(str.toString());
        fetchButton.setEnabled(true);
        resetButton.setEnabled(true);
        fetchButton.forceFocus();
      }
    });
  }

  private void resetData()
  {
    try
    {
      server.reset();
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
    placeDialogInCenter(window.getDisplay().getPrimaryMonitor().getBounds(),
                        window);
    Display display = Display.getDefault();
    while (!window.isDisposed())
    {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  public static void placeDialogInCenter(Rectangle parentSize, Shell shell)
  {
    Rectangle mySize = shell.getBounds();

    int locationX, locationY;
    locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
    locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;

    shell.setLocation(new Point(locationX, locationY));
    shell.open();
  }

  public static void main(String[] args)
  {
    final Shell window = new Shell();
    window.setSize(new Point(800, 700));
    window.setMinimumSize(new Point(800, 700));
    window.setText("Java Live Thread Analyser");

    // Fill in UI
    UI ui = new UI(window);

    // Open UI
    ui.open();
  }
}
