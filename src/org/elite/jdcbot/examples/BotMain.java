package org.elite.jdcbot.examples;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import org.elite.jdcbot.framework.User;

public class BotMain extends JFrame implements ActionListener {
	
	// logic
	private static final long serialVersionUID = 1L;
	private BotLogic logic = new BotLogic();

	// GUI
	JMenuBar mnuBar = new JMenuBar();
	JMenu mnuFile = new JMenu("File");
	JMenuItem itemExit = new JMenuItem("Exit");
	JMenu mnuHelp = new JMenu("Help");
	JMenuItem itemAbout = new JMenuItem("About");
	
	JPanel pnlServer = new JPanel();
	JLabel lblServer = new JLabel("Server : Port");
	JTextField fieldServerDC = new JTextField("192.168.179.128", 11);
	JTextField fieldServerPortDC = new JTextField("411", 3);
	JButton btnConnect = new JButton("Connect");
	JButton btnDisconnect = new JButton("Disconnect");
	
	JPanel pnlDirectories = new JPanel();
	JPanel pnlDir1 = new JPanel();
	JLabel lblDir1 = new JLabel("File path: ");
	JTextField fieldDir1 = new JTextField("C:/tmp",20);
	JPanel pnlDir2 = new JPanel();
	JLabel lblDir2 = new JLabel("File path: ");
	JTextField fieldDir2 = new JTextField("C:/tmp/Incomplete",20); 

	JTextArea textArea = new JTextArea();
	JScrollPane scrollPane = new JScrollPane(textArea);

	JPanel pnlUsers = new JPanel();
	JTextField fieldUser = new JTextField("njmartinez");
	JButton btnGetUserFiles = new JButton("Get user files");
	JTextField fieldSearch = new JTextField();
	JButton btnSearch = new JButton("Search");
	JTextField fieldFile = new JTextField();
	JButton btnDownload = new JButton("Download");
	JFileChooser chooser = new JFileChooser();
	JButton btnAddDir = new JButton("Add directory");
	JButton btnExcludeDir = new JButton("Exclude directory");
	JButton btnRemoveDir = new JButton("Remove directory");
	JButton btnProcessList = new JButton("Process list");

	JPanel pnlConfig = new JPanel();
	JLabel lblBotName = new JLabel("Bot name");
	JTextField fieldBotName = new JTextField("miBot");
	JLabel lblBotIp = new JLabel("Bot Ip");
	JTextField fieldBotIp = new JTextField("192.168.210.116");
	JLabel lblListenPort = new JLabel("Port");
	JTextField fieldListenPort = new JTextField("9000");
	JLabel lblListenPortUDP = new JLabel("UDP port");
	JTextField fieldListenPortUDP = new JTextField("10000");
	JLabel lblPassword = new JLabel("Password");
	JTextField fieldPassword = new JTextField("");
	JLabel lblDescription = new JLabel("Description");
	JTextField fieldDescription = new JTextField("Bot de pruebas");
	JLabel lblConnectionType = new JLabel("Connection Type");
	JTextField fieldConnectionType = new JTextField("ADSL");
	JLabel lblEmail = new JLabel("Email");
	JTextField fieldEmail = new JTextField("@");
	JLabel lblShareSize = new JLabel("Share size");
	JTextField fieldShareSize = new JTextField("0");
	JLabel lblUploadSlots = new JLabel("Upload slots");
	JTextField fieldUploadSlots = new JTextField("2");
	JLabel lblDownloadSlots = new JLabel("Download slots");
	JTextField fieldDownloadSlots = new JTextField("4");
	JLabel lblisPassive = new JLabel("Set mode");
	String [] states = {"Active", "Passive"};
	JComboBox cmbStates = new JComboBox(states);
	
	JTextField fieldInformation = new JTextField();

	public BotMain() {
		
		setTitle("ClienteBot Direct Connect");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(true);
		setJMenuBar(mnuBar);
		
		btnDisconnect.setEnabled(false);
		textArea.setEditable(false);
		scrollPane.setPreferredSize(new Dimension(250, 250));
		fieldInformation.setEditable(false);
		cmbStates.setSelectedIndex(1);
		
		TitledBorder pnlBorderConfig, pnlBorderServ, pnlBorderDir1, pnlBorderDir2;
		pnlBorderConfig = BorderFactory.createTitledBorder("Bot configuration");
		pnlBorderServ = BorderFactory.createTitledBorder("Quick connect");
		pnlBorderDir1 = BorderFactory.createTitledBorder("Config directory");
		pnlBorderDir2 = BorderFactory.createTitledBorder("Temporal download directory");
		pnlConfig.setBorder(pnlBorderConfig);
		pnlServer.setBorder(pnlBorderServ);
		pnlDir1.setBorder(pnlBorderDir1);
		pnlDir2.setBorder(pnlBorderDir2);

		JPanel pnlGeneral = (JPanel) this.getContentPane();

		mnuBar.add(mnuFile);
		mnuFile.add(itemExit);
		mnuBar.add(mnuHelp);
		mnuHelp.add(itemAbout);
		
		pnlServer.add(lblServer);
		pnlServer.add(fieldServerDC);
		pnlServer.add(fieldServerPortDC);
		pnlServer.add(btnConnect);
		pnlServer.add(btnDisconnect);
		
		pnlDir1.add(lblDir1);
		pnlDir1.add(fieldDir1);
		pnlDir2.add(lblDir2);
		pnlDir2.add(fieldDir2);
		pnlDirectories.add(pnlDir1);
		pnlDirectories.add(pnlDir2);
		
		JTabbedPane tabbedPaneS = new JTabbedPane();
		tabbedPaneS.addTab("Server", pnlServer);
		tabbedPaneS.addTab("Directories", pnlDirectories);
		
		pnlUsers.setLayout(new GridLayout(0, 2));
		pnlUsers.add(fieldUser);
		pnlUsers.add(btnGetUserFiles);
		pnlUsers.add(fieldSearch);
		pnlUsers.add(btnSearch);
		pnlUsers.add(fieldFile);
		pnlUsers.add(btnDownload);
		pnlUsers.add(btnAddDir);
		pnlUsers.add(btnExcludeDir);
		pnlUsers.add(btnRemoveDir);
		pnlUsers.add(btnProcessList);
		
		pnlConfig.setLayout(new GridLayout(0, 2));
		pnlConfig.add(lblBotName);
		pnlConfig.add(fieldBotName);
		pnlConfig.add(lblBotIp);
		pnlConfig.add(fieldBotIp);
		pnlConfig.add(lblListenPort);
		pnlConfig.add(fieldListenPort);
		pnlConfig.add(lblListenPortUDP);
		pnlConfig.add(fieldListenPortUDP);
		pnlConfig.add(lblPassword);
		pnlConfig.add(fieldPassword);
		pnlConfig.add(lblDescription);
		pnlConfig.add(fieldDescription);
		pnlConfig.add(lblConnectionType);
		pnlConfig.add(fieldConnectionType);
		pnlConfig.add(lblEmail);
		pnlConfig.add(fieldEmail);
		pnlConfig.add(lblShareSize);
		pnlConfig.add(fieldShareSize);
		pnlConfig.add(lblUploadSlots);
		pnlConfig.add(fieldUploadSlots);
		pnlConfig.add(lblDownloadSlots);
		pnlConfig.add(fieldDownloadSlots);
		pnlConfig.add(lblisPassive);
		pnlConfig.add(cmbStates);		
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Actions", pnlUsers);
		tabbedPane.addTab("Log", scrollPane);
		
		pnlGeneral.add("North", tabbedPaneS);
		pnlGeneral.add("Center", tabbedPane);
		pnlGeneral.add("West", pnlConfig);
		pnlGeneral.add("South", fieldInformation);
		setContentPane(pnlGeneral);
		pack();

		itemExit.addActionListener(this);
		btnConnect.addActionListener(this);
		btnDisconnect.addActionListener(this);
		btnGetUserFiles.addActionListener(this);
		btnSearch.addActionListener(this);
		btnDownload.addActionListener(this);
		btnAddDir.addActionListener(this);
		btnExcludeDir.addActionListener(this);
		btnRemoveDir.addActionListener(this);
		btnProcessList.addActionListener(this);
		
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
	}

	// handles the buttons action
	public void actionPerformed(ActionEvent ae) {
		SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
		Calendar calendarNow = Calendar.getInstance();
		
		if(ae.getSource() == itemExit){
			if(btnDisconnect.isEnabled()){
				logic.disconnect();
			}
			System.exit(0);
		}
		else if (ae.getSource() == btnConnect) {
			boolean isActive = false;
			if(cmbStates.getSelectedItem() == "Active"){
				isActive = true;
			}
			else if (cmbStates.getSelectedItem() == "Passive"){
				isActive = false;
			}
			logic.configBot(fieldBotName.getText(), fieldBotIp.getText(),
					Integer.parseInt(fieldListenPort.getText()),
					Integer.parseInt(fieldListenPortUDP.getText()),
					fieldPassword.getText(), fieldDescription.getText(),
					fieldConnectionType.getText() + User.NORMAL_FLAG,
					fieldEmail.getText(), fieldShareSize.getText(),
					Integer.parseInt(fieldUploadSlots.getText()),
					Integer.parseInt(fieldDownloadSlots.getText()),
					isActive, fieldDir1.getText(), fieldDir2.getText());
			logic.connect(fieldServerDC.getText(), Integer.parseInt(fieldServerPortDC.getText()));
			btnConnect.setEnabled(false);
			btnDisconnect.setEnabled(true);
			textArea.append(formatter.format(calendarNow.getTime()) + ": Is connected now\n");
			fieldInformation.setText("Connected");
		} else if (ae.getSource() == btnDisconnect) {
			logic.disconnect();
			btnConnect.setEnabled(true);
			btnDisconnect.setEnabled(false);
			textArea.append(formatter.format(calendarNow.getTime()) + ": Is disconnected now\n");
			fieldInformation.setText("Disconnected");
		} else if (ae.getSource() == btnGetUserFiles) {
			logic.readFiles(fieldUser.getText());
			textArea.append(formatter.format(calendarNow.getTime()) + ": Get user files finished\n");
			fieldInformation.setText("Get user files finished");
		} else if (ae.getSource() == btnSearch) {
			String searchText = fieldSearch.getText();
			if (searchText.isEmpty()) {
				fieldInformation.setText("Enter a word to start searching");
			} else {
				logic.search(searchText);
				textArea.append(formatter.format(calendarNow.getTime()) + ": Search finished\n");
				fieldInformation.setText("Search finished");
			}
		} else if (ae.getSource()==btnDownload){
			logic.downloadFile(fieldFile.getText());
			textArea.append(formatter.format(calendarNow.getTime()) + ": Starting download process\n");
			fieldInformation.setText("Processing a download");
		}else if(ae.getSource() == btnAddDir){		
			int selection = chooser.showOpenDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION) {
				String directory = chooser.getSelectedFile().toString();
				logic.addDirectory(directory);
				JOptionPane.showMessageDialog(getRootPane(), "This folder has been added to the list:\n" + directory);
				textArea.append(formatter.format(calendarNow.getTime())+": This folder has been added to the list: "+ directory+"\n");
				fieldInformation.setText("Folder adicionado");
			}	  
		} else if(ae.getSource() == btnExcludeDir){
			int selection = chooser.showOpenDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION) {
				String directory = chooser.getSelectedFile().toString();
				logic.excludeDirectory(directory);
				JOptionPane.showMessageDialog(getRootPane(), "This folder has beend excluded from the list:\n" + directory);
				textArea.append(formatter.format(calendarNow.getTime())+": This folder has beend excluded from the list: "+ directory+"\n");
				fieldInformation.setText("Folder excluido");
			}
		} else if(ae.getSource() == btnRemoveDir){
			int selection = chooser.showOpenDialog(this);
			if (selection == JFileChooser.APPROVE_OPTION) {
				String directory = chooser.getSelectedFile().toString();
				logic.removeDirectory(directory);
				JOptionPane.showMessageDialog(getRootPane(), "This folder has been removed from the list:\n" + directory);
				textArea.append(formatter.format(calendarNow.getTime())+": This folder has been removed from the list: "+ directory+"\n");
				fieldInformation.setText("Folder retirado");
			}
		} else if(ae.getSource() == btnProcessList){
			logic.processList();
			JOptionPane.showMessageDialog(getRootPane(), "The list has been processed");
			textArea.append(formatter.format(calendarNow.getTime())+": The list has been updated\n");
			fieldInformation.setText("List updated");
		}
	}

	// main
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		BotMain clienteBot = new BotMain();
		clienteBot.setVisible(true);
	}	
}
