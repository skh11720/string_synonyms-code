package snu.kdd.synonym.synonymRev.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

public class GuiMain extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3451963678342390680L;
	static private final String newline = "\n";

	private final JButton btnLoadData = new JButton( "Load Data" );
	private final JButton btnTransaction = new JButton( "Transaction" );
	private final JFileChooser fc = new JFileChooser( new File( "./data" ) );
	private final JList<String> contentList = new JList<String>();
	private final JTextArea logArea = new JTextArea();
	private final JCheckBox chckbxVerbose = new JCheckBox( "Verbose" );
	RunAlgorithmThread thread;

	private String dataFilePath = null;
	private String ruleFilePath = null;

	private final JButton btnRules = new JButton( "Rules" );
	private final JButton btnJoinNaive = new JButton( "JoinNaive" );
	private final JButton btnJoinBK = new JButton( "JoinBK" );
	private final JButton btnJoinMH = new JButton( "JoinMH" );
	private final JButton btnDebug = new JButton( "JoinDebug" );
	private final JButton btnJoinMin = new JButton( "JoinMin" );
	private final JButton btnJoinHybridOpt = new JButton( "JoinHybridOpt" );
	private final JButton btnJoinBK_SP = new JButton( "JoinBKSP" );
	private final JButton btnPrintInfo = new JButton( "PrintInfo" );

	private JTextField sampleTextField;
	private JTextField thresTextField;
	private JTextField idTextField;

	// private class createActionListener implements ActionListener {
	// @Override
	// public void actionPerformed( ActionEvent e ) {
	// GenDataOption option = new GenDataOption();
	// int result = JOptionPane.showConfirmDialog( null, option, "Test", JOptionPane.OK_CANCEL_OPTION,
	// JOptionPane.PLAIN_MESSAGE );
	// if( result == JOptionPane.OK_OPTION ) {
	// DataInfo info = new DataInfo( option );
	//
	// try {
	// File dir = new File( "data/" + info.name );
	// boolean successful = dir.mkdir();
	// if( successful ) {
	//
	// DataBuilder.createDataSet( info, "data/" + info.name );
	// logArea.append( "Data generated: " + info.fileSize + " bytes" );
	// }
	// else {
	// logArea.append( "Directory already exists" );
	// }
	// }
	// catch( IOException except ) {
	// logArea.append( except.toString() );
	// }
	// }
	// else {
	// logArea.append( "Cancelled" );
	// }
	// }
	// }

	private class LoadDataActionListener implements ActionListener {
		@Override
		public void actionPerformed( ActionEvent e ) {
			fc.setFileSelectionMode( JFileChooser.FILES_ONLY );

			int returnVal = fc.showOpenDialog( GuiMain.this );

			if( returnVal == JFileChooser.APPROVE_OPTION ) {
				File file = fc.getSelectedFile();
				// This is where a real application would open the file.
				logArea.append( "Opening: " + file.getName() + "." + newline );

				dataFilePath = file.getAbsolutePath();

				// dataInfo = DataBuilder.loadData( filePath, tList, rList );
				// dataInfo.name = filePath;

				if( ruleFilePath != null ) {
					btnTransaction.setEnabled( true );
					btnRules.setEnabled( true );
					btnJoinNaive.setEnabled( true );
					btnJoinBK.setEnabled( true );
					btnJoinMH.setEnabled( true );
					btnJoinMin.setEnabled( true );
					btnJoinHybridOpt.setEnabled( true );
					btnJoinBK_SP.setEnabled( true );
				}
			}
			else {
				logArea.append( "Open command cancelled by user." + newline );
			}
		}
	}

	private class LoadRuleActionListener implements ActionListener {
		@Override
		public void actionPerformed( ActionEvent e ) {
			fc.setFileSelectionMode( JFileChooser.FILES_ONLY );

			int returnVal = fc.showOpenDialog( GuiMain.this );

			if( returnVal == JFileChooser.APPROVE_OPTION ) {
				File file = fc.getSelectedFile();
				// This is where a real application would open the file.
				logArea.append( "Opening: " + file.getName() + " as rule.\n" );

				ruleFilePath = file.getAbsolutePath();

				if( dataFilePath != null ) {
					btnTransaction.setEnabled( true );
					btnRules.setEnabled( true );
					btnJoinNaive.setEnabled( true );
					btnJoinBK.setEnabled( true );
					btnJoinMH.setEnabled( true );
					btnJoinMin.setEnabled( true );
					btnJoinHybridOpt.setEnabled( true );
					btnJoinBK_SP.setEnabled( true );
					btnDebug.setEnabled( true );
					btnPrintInfo.setEnabled( true );
				}
			}
			else {
				logArea.append( "Open command cancelled by user." + newline );
			}
		}
	}

	private class RunAlgorithmActionListener implements ActionListener {
		@Override
		public void actionPerformed( ActionEvent evt ) {
			if( thread != null && thread.isAlive() ) {
				logArea.append( "Thread is already running" );
			}

			String arg = "-dataOnePath " + dataFilePath + " -dataTwoPath " + dataFilePath + " -rulePath " + ruleFilePath
					+ " -outputPath output -oneSideJoin False";
			String additional = null;

			String command = evt.getActionCommand();
			if( command.equals( "JoinNaive" ) ) {
				arg = arg + " -algorithm JoinNaive -additional";
				additional = "-1";
			}
			else if( command.equals( "JoinBK" ) ) {
				arg = arg + " -algorithm JoinBK -additional";
				additional = "-K 2 -qSize 3";
			}
			else if( command.equals( "JoinMH" ) ) {
				arg = arg + " -algorithm JoinMH -additional";
				additional = "-K 2 -qSize 1";
			}
			else if( command.equals( "JoinDebug" ) ) {
				// String sample = sampleTextField.getText();
				// arg = arg + " -algorithm DebugAlg -additional";
				// additional = "-compact -v TopDownHashSetSinglePathDS 0 -s " + sample;
				arg = arg + " -algorithm JoinMinRange -additional";
				additional = "-K 1 -qSize 2";
			}
			else if( command.equals( "JoinMin" ) ) {
				arg = arg + " -algorithm JoinMin -additional";
				additional = "-K 2 -qSize 2";
			}
			else if( command.equals( "JoinHybridOpt" ) ) {
				String sample = sampleTextField.getText();
				arg = arg + " -algorithm JoinHybridOpt -additional";
				// TODO:: compact option is not implemented and it is not clear which option it is
				// additional = "-compact -v TopDownHashSetSinglePathDS 0 -s " + sample;
				additional = "TopDownHashSetSinglePathDS 0 -sample " + sample;
			}
			else if( command.equals( "JoinBKSP" ) ) {
				arg = arg + " -algorithm JoinBK -split -additional";
				additional = "-K 2 -qSize 2";
			}

			System.out.println( "Args: " + arg );

			boolean verbose = chckbxVerbose.isSelected();

			thread = new RunAlgorithmThread( arg, additional, verbose, logArea );
			logArea.append( "Starting\n" );
			thread.start();
		}

	}

	public GuiMain() {
		setTitle( "Sequence Miner" );
		setSize( new Dimension( 835, 689 ) );
		setPreferredSize( new Dimension( 700, 650 ) );
		getContentPane().setLayout( null );

		btnLoadData.addActionListener( new LoadDataActionListener() );
		btnLoadData.setBounds( 139, 12, 120, 23 );

		getContentPane().add( btnLoadData );
		btnTransaction.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
			}
		} );
		btnTransaction.setEnabled( false );
		btnTransaction.setBounds( 394, 12, 120, 23 );
		// btnTransaction.addActionListener( new showTransactionListener() );
		getContentPane().add( btnTransaction );

		JButton btnCreateData = new JButton( "Create Data" );
		// btnCreateData.addActionListener( new createActionListener() );
		btnCreateData.setBounds( 12, 12, 120, 23 );
		getContentPane().add( btnCreateData );

		btnRules.setEnabled( false );
		btnRules.setBounds( 523, 12, 120, 23 );
		// btnRules.addActionListener( new showRuleListener() );

		getContentPane().add( btnRules );
		RunAlgorithmActionListener algorithmAction = new RunAlgorithmActionListener();
		btnJoinNaive.addActionListener( algorithmAction );
		btnJoinNaive.setEnabled( false );
		btnJoinNaive.setBounds( 12, 43, 120, 23 );

		getContentPane().add( btnJoinNaive );
		btnJoinMH.addActionListener( algorithmAction );
		btnJoinMH.setEnabled( false );
		btnJoinMH.setBounds( 267, 43, 120, 23 );

		getContentPane().add( btnJoinMH );

		btnJoinHybridOpt.addActionListener( algorithmAction );
		btnJoinHybridOpt.setEnabled( false );
		btnJoinHybridOpt.setBounds( 12, 74, 120, 23 );
		getContentPane().add( btnJoinHybridOpt );

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds( 12, 536, 795, 104 );
		getContentPane().add( scrollPane );
		logArea.setEditable( false );
		scrollPane.setViewportView( logArea );

		logArea.setTabSize( 4 );
		logArea.setLineWrap( true );
		DefaultCaret caret = (DefaultCaret) logArea.getCaret();
		caret.setUpdatePolicy( DefaultCaret.ALWAYS_UPDATE );

		btnJoinMin.addActionListener( algorithmAction );
		btnJoinMin.setEnabled( false );
		btnJoinMin.setBounds( 394, 43, 120, 23 );
		getContentPane().add( btnJoinMin );

		chckbxVerbose.setBounds( 655, 43, 73, 23 );
		getContentPane().add( chckbxVerbose );

		sampleTextField = new JTextField();
		sampleTextField.setText( "0.1" );
		sampleTextField.setBounds( 338, 76, 48, 21 );
		getContentPane().add( sampleTextField );
		sampleTextField.setColumns( 10 );

		JLabel lblSampling = new JLabel( "Sampling" );
		lblSampling.setBounds( 276, 79, 58, 15 );
		getContentPane().add( lblSampling );

		JLabel lblThres = new JLabel( "Thres" );
		lblThres.setBounds( 398, 79, 33, 15 );
		getContentPane().add( lblThres );

		thresTextField = new JTextField();
		thresTextField.setText( "3" );
		thresTextField.setBounds( 443, 76, 48, 21 );
		getContentPane().add( thresTextField );
		thresTextField.setColumns( 10 );

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds( 12, 104, 795, 422 );
		getContentPane().add( scrollPane_1 );
		scrollPane_1.setViewportView( contentList );

		JButton btnLoadRule = new JButton( "Load Rule" );
		btnLoadRule.addActionListener( new LoadRuleActionListener() );
		btnLoadRule.setBounds( 267, 12, 120, 23 );
		getContentPane().add( btnLoadRule );

		btnJoinBK_SP.setEnabled( false );
		btnJoinBK_SP.setBounds( 139, 74, 120, 23 );
		getContentPane().add( btnJoinBK_SP );
		btnJoinBK_SP.addActionListener( algorithmAction );

		btnJoinBK.setEnabled( false );
		btnJoinBK.addActionListener( algorithmAction );
		btnJoinBK.setBounds( 139, 43, 120, 23 );
		getContentPane().add( btnJoinBK );

		btnDebug.setEnabled( false );
		btnDebug.addActionListener( algorithmAction );
		btnDebug.setBounds( 523, 43, 120, 23 );
		getContentPane().add( btnDebug );

		JLabel lblRecordId = new JLabel( "ID" );
		lblRecordId.setBounds( 655, 77, 33, 15 );
		getContentPane().add( lblRecordId );

		idTextField = new JTextField();
		idTextField.setText( "0" );
		idTextField.setColumns( 10 );
		idTextField.setBounds( 700, 74, 48, 21 );
		getContentPane().add( idTextField );
	}

	public static void main( String args[] ) {
		GuiMain main = new GuiMain();
		main.setVisible( true );
		main.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
}
