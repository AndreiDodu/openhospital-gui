/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2020 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isf.medicals.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.medicals.gui.MedicalEdit.MedicalListener;
import org.isf.medicals.manager.MedicalBrowsingManager;
import org.isf.medicals.model.Medical;
import org.isf.medtype.manager.MedicalTypeBrowserManager;
import org.isf.medtype.model.MedicalType;
import org.isf.menu.gui.MainMenu;
import org.isf.menu.manager.Context;
import org.isf.stat.gui.report.GenericReportFromDateToDate;
import org.isf.stat.gui.report.GenericReportPharmaceuticalOrder;
import org.isf.stat.gui.report.GenericReportPharmaceuticalStock;
import org.isf.stat.gui.report.GenericReportPharmaceuticalStockCard;
import org.isf.utils.excel.ExcelExporter;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.CustomJDateChooser;
import org.isf.utils.jobjects.JFromDateToDateChooserDialog;
import org.isf.utils.jobjects.JMonthYearChooser;
import org.isf.utils.jobjects.ModalJFrame;
import org.isf.utils.time.TimeTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class shows a complete extended list of medical drugs,
 * supplies-sundries, diagnostic kits -reagents, laboratory chemicals. It is
 * possible to filter data with a selection combo box
 * and edit-insert-delete records
 *
 * @author bob
 * 11-dic-2005
 * modified by alex:
 * - product code
 * - pieces per packet
 */
public class MedicalBrowser extends ModalJFrame implements MedicalListener { // implements RowSorterListener{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(MedicalBrowser.class);

	public void medicalInserted(Medical medical) {
		pMedicals.add(0, medical);
		((MedicalBrowsingModel) table.getModel()).fireTableDataChanged();
		table.updateUI();
		if (table.getRowCount() > 0)
			table.setRowSelectionInterval(0, 0);
		repaint();
	}

	public void medicalUpdated(AWTEvent e) {
		pMedicals.set(selectedrow, medical);
		((MedicalBrowsingModel) table.getModel()).fireTableDataChanged();
		table.updateUI();
		if ((table.getRowCount() > 0) && selectedrow > -1)
			table.setRowSelectionInterval(selectedrow, selectedrow);
		repaint();

	}

	private static final int DEFAULT_WIDTH = 500;
	private static final int DEFAULT_HEIGHT = 400;
	private int pfrmWidth;
	private int pfrmHeight;
	private int selectedrow;
	private JComboBox pbox;
	private ArrayList<Medical> pMedicals;
	private String[] pColums = {
			MessageBundle.getMessage("angal.medicals.typem"),
			MessageBundle.getMessage("angal.common.codem"),
			MessageBundle.getMessage("angal.common.descriptionm"),
			MessageBundle.getMessage("angal.medicals.pcsperpckm"),
			MessageBundle.getMessage("angal.medicals.stockm"),
			MessageBundle.getMessage("angal.medicals.critlevelm"),
			MessageBundle.getMessage("angal.medicals.outofstockm")
	};
	private String[] pColumsSorter = { "MDSRT_DESC", "MDSR_CODE", "MDSR_DESC", null, "STOCK", "MDSR_MIN_STOCK_QTI", "STOCK" };
	private boolean[] pColumsNormalSorting = { true, true, true, true, true, true, false };
	private int[] pColumwidth = { 100, 100, 400, 60, 60, 80, 100 };
	private boolean[] pColumResizable = { true, true, true, true, true, true, true };
	private Medical medical;
	private DefaultTableModel model;
	private JTable table;
	private final JFrame me;

	private String pSelection;
	private JTextField searchString = null;
	protected boolean altKeyReleased = true;
	private String lastKey = "";
	private JButton buttonAMC;

	private MedicalTypeBrowserManager medicalTypeManager = Context.getApplicationContext().getBean(MedicalTypeBrowserManager.class);
	private MedicalBrowsingManager medicalBrowsingManager = Context.getApplicationContext().getBean(MedicalBrowsingManager.class);

	private void filterMedical(String key) {
		model = new MedicalBrowsingModel(key, false);
		table.setModel(model);
		searchString.requestFocus();
	}

	public MedicalBrowser() {
		me = this;
		setTitle(MessageBundle.getMessage("angal.medicals.pharmaceuticalbrowsing"));
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screensize = kit.getScreenSize();
		pfrmWidth = 940; //screensize.width / 2;
		pfrmHeight = screensize.height / 2;
		setBounds((screensize.width - pfrmWidth) / 2, screensize.height / 4, pfrmWidth,
				pfrmHeight);
		setContentPane(getContentpane());
		pack();
		setVisible(true);
		setLocationRelativeTo(null);
		searchString.requestFocus();
	}

	private JPanel getContentpane() {
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(getScrollPane(), BorderLayout.CENTER);
		contentPane.add(getJButtonPanel(), BorderLayout.SOUTH);
		return contentPane;
	}

	private JScrollPane getScrollPane() {
		JScrollPane scrollPane = new JScrollPane(getJTable());
		int totWidth = 0;
		for (int colWidth : pColumwidth) {
			totWidth += colWidth;
		}
		scrollPane.setPreferredSize(new Dimension(totWidth, 450));
		return scrollPane;
	}

	private JTable getJTable() {
		if (table == null) {
			model = new MedicalBrowsingModel();
			table = new JTable(model);
			table.setAutoCreateRowSorter(true);
			table.setDefaultRenderer(Object.class, new ColorTableCellRenderer());
			for (int i = 0; i < pColumwidth.length; i++) {
				table.getColumnModel().getColumn(i).setMinWidth(pColumwidth[i]);
				if (!pColumResizable[i])
					table.getColumnModel().getColumn(i).setMaxWidth(pColumwidth[i]);
			}
		}
		return table;
	}

	private JPanel getJButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JLabel(MessageBundle.getMessage("angal.medicals.selecttype")));
		buttonPanel.add(getComboBoxMedicalType());
		buttonPanel.add(getSearchBox());
		if (MainMenu.checkUserGrants("btnpharmaceuticalnew"))
			buttonPanel.add(getJButtonNew());
		if (MainMenu.checkUserGrants("btnpharmaceuticaledit"))
			buttonPanel.add(getJButtonEdit());
		if (MainMenu.checkUserGrants("btnpharmaceuticaldel"))
			buttonPanel.add(getJButtonDelete());
		buttonPanel.add(getJButtonReport());
		buttonPanel.add(getJButtonStock());
		buttonPanel.add(getJButtonStockCard());
		buttonPanel.add(getJButtonOrderList());
		buttonPanel.add(getJButtonExpiring());
		buttonPanel.add(getJButtonAMC());
		buttonPanel.add(getJButtonClose());
		return buttonPanel;
	}

	private JButton getJButtonAMC() {
		if (buttonAMC == null) {
			buttonAMC = new JButton(MessageBundle.getMessage("angal.medicals.averagemonthlyconsumption.abbr"));
			buttonAMC.setMnemonic(KeyEvent.VK_M);
			buttonAMC.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent event) {
					new GenericReportPharmaceuticalOrder(GeneralData.PHARMACEUTICALAMC);

				}
			});
		}
		return buttonAMC;
	}

	private JTextField getSearchBox() {
		if (searchString == null) {
			searchString = new JTextField();
			searchString.setColumns(15);
			searchString.addKeyListener(new KeyListener() {

				public void keyTyped(KeyEvent e) {
					if (altKeyReleased) {
						lastKey = "";
						String s = "" + e.getKeyChar();
						if (Character.isLetterOrDigit(e.getKeyChar())) {
							lastKey = s;
						}
						filterMedical(searchString.getText());
					}
				}

				public void keyPressed(KeyEvent e) {
					int key = e.getKeyCode();
					if (key == KeyEvent.VK_ALT)
						altKeyReleased = false;
				}

				public void keyReleased(KeyEvent e) {
					altKeyReleased = true;
				}
			});
		}
		return searchString;
	}

	private JButton getJButtonClose() {
		JButton buttonClose = new JButton(MessageBundle.getMessage("angal.common.close"));
		buttonClose.setMnemonic(KeyEvent.VK_C);
		buttonClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		return buttonClose;
	}

	private JButton getJButtonExpiring() {
		JButton buttonExpiring = new JButton(MessageBundle.getMessage("angal.medicals.expiring"));
		buttonExpiring.setMnemonic(KeyEvent.VK_X);
		buttonExpiring.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				launchExpiringReport();

			}
		});
		return buttonExpiring;
	}

	private JButton getJButtonOrderList() {
		JButton buttonOrderList = new JButton(MessageBundle.getMessage("angal.medicals.orderlist"));
		buttonOrderList.setMnemonic(KeyEvent.VK_O);
		buttonOrderList.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				new GenericReportPharmaceuticalOrder(GeneralData.PHARMACEUTICALORDER);
			}
		});
		return buttonOrderList;
	}

	private JButton getJButtonStock() {
		JButton buttonStock = new JButton(MessageBundle.getMessage("angal.medicals.stock"));
		buttonStock.setMnemonic(KeyEvent.VK_S);
		buttonStock.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {

				ArrayList<String> dateOptions = new ArrayList<String>();
				dateOptions.add(MessageBundle.getMessage("angal.medicals.today"));
				dateOptions.add(MessageBundle.getMessage("angal.common.date"));

				Icon icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$
				String dateOption = (String) JOptionPane.showInputDialog(MedicalBrowser.this,
						MessageBundle.getMessage("angal.medicals.pleaseselectareport"),
						MessageBundle.getMessage("angal.medicals.report"),
						JOptionPane.INFORMATION_MESSAGE,
						icon,
						dateOptions.toArray(),
						dateOptions.get(0));

				if (dateOption == null)
					return;

				ArrayList<String> lotOptions = new ArrayList<String>();
				lotOptions.add(MessageBundle.getMessage("angal.medicals.onlyquantity"));
				lotOptions.add(MessageBundle.getMessage("angal.medicals.withlot"));

				String lotOption = (String) JOptionPane.showInputDialog(MedicalBrowser.this,
						MessageBundle.getMessage("angal.medicals.pleaseselectareport"),
						MessageBundle.getMessage("angal.medicals.report"),
						JOptionPane.INFORMATION_MESSAGE,
						icon,
						lotOptions.toArray(),
						lotOptions.get(0));


				/* Getting Report parameters */
				String sortBy = null;
				String groupBy = null;
				String filter = "%" + searchString.getText() + "%";
				if (pbox.getSelectedItem() instanceof MedicalType)
					groupBy = ((MedicalType) pbox.getSelectedItem()).getDescription();
				//System.out.println("==> GROUPING : " + groupBy);
				List<?> sortedKeys = table.getRowSorter().getSortKeys();
				if (!sortedKeys.isEmpty()) {
					int sortedColumn = ((SortKey) sortedKeys.get(0)).getColumn();
					SortOrder sortedOrder = ((SortKey) sortedKeys.get(0)).getSortOrder();

					String columnName = pColumsSorter[sortedColumn];
					String columnOrder = sortedOrder.toString().equals("ASCENDING") ? "ASC" : "DESC";
					if (!pColumsNormalSorting[sortedColumn])
						columnOrder = sortedOrder.toString().equals("ASCENDING") ? "DESC" : "ASC";
					if (groupBy == null) {
						groupBy = "%";
						sortBy = "MDSRT_DESC, " + columnName + " " + columnOrder;
					} else
						sortBy = columnName + " " + columnOrder;

				} else { //default values
					groupBy = "%%";
					sortBy = "MDSRT_DESC, MDSR_DESC";
				}

				String report = "";

				int i = 0;
				if (lotOptions.indexOf(lotOption) == i) {
					report = GeneralData.PHARMACEUTICALSTOCK;
				}
				if (lotOptions.indexOf(lotOption) == ++i) {
					report = GeneralData.PHARMACEUTICALSTOCKLOT;
				}
				i = 0;
				if (dateOptions.indexOf(dateOption) == i) {
					new GenericReportPharmaceuticalStock(null, report, filter, groupBy, sortBy, false);
					new GenericReportPharmaceuticalStock(null, report, filter, groupBy, sortBy, true);
					return;
				}
				if (dateOptions.indexOf(dateOption) == ++i) {

					icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$

					CustomJDateChooser dateChooser = new CustomJDateChooser();
					dateChooser.setLocale(new Locale(GeneralData.LANGUAGE));

					int r = JOptionPane.showConfirmDialog(MedicalBrowser.this,
							dateChooser,
							MessageBundle.getMessage("angal.common.date"),
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							icon);

					if (r == JOptionPane.OK_OPTION) {
						new GenericReportPharmaceuticalStock(dateChooser.getDate(), report, filter, groupBy, sortBy, false);
						new GenericReportPharmaceuticalStock(dateChooser.getDate(), report, filter, groupBy, sortBy, true);
						return;

					} else {
						return;
					}
				}
			}
		});
		return buttonStock;
	}

	private JButton getJButtonStockCard() {
		JButton buttonStockCard = new JButton(MessageBundle.getMessage("angal.medicals.stockcard"));
		buttonStockCard.setMnemonic(KeyEvent.VK_K);
		buttonStockCard.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (table.getSelectedRow() < 0) {
					JOptionPane.showMessageDialog(
							MedicalBrowser.this,
							MessageBundle.getMessage("angal.common.pleaseselectarow"),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.PLAIN_MESSAGE);
					return;
				} else {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					Medical medical = (Medical) (((MedicalBrowsingModel) model).getValueAt(selectedrow, -1));

					// Select Dates
					JFromDateToDateChooserDialog dataRange = new JFromDateToDateChooserDialog(MedicalBrowser.this);
					dataRange.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dataRange.setVisible(true);

					Date dateFrom = dataRange.getDateFrom();
					Date dateTo = dataRange.getDateTo();
					boolean toExcel = dataRange.isExcel();

					if (!dataRange.isCancel()) {
						new GenericReportPharmaceuticalStockCard("ProductLedger", dateFrom, dateTo, medical, null, toExcel);
						return;
					}
				}
			}
		});
		return buttonStockCard;
	}

	private JButton getJButtonReport() {
		JButton buttonExport = new JButton(MessageBundle.getMessage("angal.medicals.export"));
		buttonExport.setMnemonic(KeyEvent.VK_X);
		buttonExport.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {

				String fileName = compileFileName();
				File defaultFileName = new File(fileName);
				JFileChooser fcExcel = ExcelExporter.getJFileChooserExcel(defaultFileName);

				int iRetVal = fcExcel.showSaveDialog(MedicalBrowser.this);
				if (iRetVal == JFileChooser.APPROVE_OPTION) {
					File exportFile = fcExcel.getSelectedFile();
					if (!exportFile.getName().endsWith("xls"))
						exportFile = new File(exportFile.getAbsoluteFile() + ".xls");
					ExcelExporter xlsExport = new ExcelExporter();
					try {
						xlsExport.exportTableToExcel(table, exportFile);
					} catch (IOException exc) {
						JOptionPane.showMessageDialog(MedicalBrowser.this,
								exc.getMessage(),
								MessageBundle.getMessage("angal.hospital"),
								JOptionPane.PLAIN_MESSAGE);
						logger.error("Export to excel error : {}", exc.getMessage());
					}
				}
			}
		});
		return buttonExport;
	}

	private String compileFileName() {
		StringBuilder filename = new StringBuilder("Stock");
		if (pbox.isEnabled()
				&& !pbox.getSelectedItem().equals(
				MessageBundle.getMessage("angal.medicals.allm"))) {

			filename.append("_").append(pbox.getSelectedItem());
		}
		return filename.toString();
	}

	private JButton getJButtonDelete() {
		JButton buttonDelete = new JButton(MessageBundle.getMessage("angal.common.delete"));
		buttonDelete.setMnemonic(KeyEvent.VK_D);
		buttonDelete.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (table.getSelectedRow() < 0) {
					JOptionPane.showMessageDialog(
							MedicalBrowser.this,
							MessageBundle.getMessage("angal.common.pleaseselectarow"),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.PLAIN_MESSAGE);
					return;
				} else {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					Medical med = (Medical) (((MedicalBrowsingModel) model).getValueAt(selectedrow, -1));
					StringBuilder deleteMessage = new StringBuilder()
							.append(MessageBundle.getMessage("angal.medicals.deletemedical"))
							.append(" \"")
							.append(med.getDescription())
							.append("\" ?");
					int n = JOptionPane.showConfirmDialog(
							MedicalBrowser.this,
							deleteMessage.toString(),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.YES_NO_OPTION);
					boolean deleted;
					try {
						deleted = medicalBrowsingManager.deleteMedical(med);
					} catch (OHServiceException e) {
						deleted = false;
						OHServiceExceptionUtil.showMessages(e);
					}
					if ((n == JOptionPane.YES_OPTION) && deleted) {
						pMedicals.remove(selectedrow);
						model.fireTableDataChanged();
						table.updateUI();
					}
				}
			}
		});
		return buttonDelete;
	}

	private JButton getJButtonEdit() {
		JButton buttonEdit = new JButton(MessageBundle.getMessage("angal.common.edit"));
		buttonEdit.setMnemonic(KeyEvent.VK_E);
		buttonEdit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (table.getSelectedRow() < 0) {
					JOptionPane.showMessageDialog(
							MedicalBrowser.this,
							MessageBundle.getMessage("angal.common.pleaseselectarow"),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.PLAIN_MESSAGE);
					return;
				} else {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					medical = (Medical) (((MedicalBrowsingModel) model).getValueAt(selectedrow, -1));
					MedicalEdit editrecord = new MedicalEdit(medical, false, me);
					editrecord.addMedicalListener(MedicalBrowser.this);
					editrecord.setVisible(true);
				}
			}
		});
		return buttonEdit;
	}

	private JButton getJButtonNew() {
		JButton buttonNew = new JButton(MessageBundle.getMessage("angal.common.new"));
		buttonNew.setMnemonic(KeyEvent.VK_N);
		buttonNew.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				// medical will reference the new record
				medical = new Medical(null, new MedicalType("", ""), "", "", 0, 0, 0, 0, 0);
				MedicalEdit newrecord = new MedicalEdit(medical, true, me);
				newrecord.addMedicalListener(MedicalBrowser.this);
				newrecord.setVisible(true);
			}
		});
		return buttonNew;
	}

	private JComboBox getComboBoxMedicalType() {
		if (pbox == null) {
			pbox = new JComboBox();
			pbox.addItem(MessageBundle.getMessage("angal.medicals.allm"));
			ArrayList<MedicalType> type;
			try {
				type = medicalTypeManager.getMedicalType();
				for (MedicalType elem : type) {
					pbox.addItem(elem);
				}
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
		}
		pbox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				pSelection = pbox.getSelectedItem().toString();
				if (pSelection.compareTo(MessageBundle.getMessage("angal.medicals.allm")) == 0) {
					model = new MedicalBrowsingModel();
					table.setModel(model);
				} else {
					model = new MedicalBrowsingModel(pSelection, true);
					table.setModel(model);
				}
				model.fireTableDataChanged();
				table.updateUI();
			}
		});
		return pbox;
	}

	protected void launchExpiringReport() {

		ArrayList<String> options = new ArrayList<String>();
		options.add(MessageBundle.getMessage("angal.medicals.today"));
		options.add(MessageBundle.getMessage("angal.medicals.thismonth"));
		options.add(MessageBundle.getMessage("angal.medicals.nextmonth"));
		options.add(MessageBundle.getMessage("angal.medicals.nexttwomonths"));
		options.add(MessageBundle.getMessage("angal.medicals.nextthreemonths"));
		options.add(MessageBundle.getMessage("angal.medicals.othermonth"));

		Icon icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$
		String option = (String) JOptionPane.showInputDialog(MedicalBrowser.this,
				MessageBundle.getMessage("angal.medicals.pleaseselectperiod"),
				MessageBundle.getMessage("angal.medicals.expiringreport"),
				JOptionPane.INFORMATION_MESSAGE,
				icon,
				options.toArray(),
				options.get(0));

		if (option == null)
			return;

		GregorianCalendar from = new GregorianCalendar();
		GregorianCalendar to = new GregorianCalendar();
		to.setTime(from.getTime());

		int i = 0;

		if (options.indexOf(option) == i) {
			//today, nothing to change
			//to.setTime(from.getTime());
		}
		if (options.indexOf(option) == ++i) {
			//this month
			from.set(GregorianCalendar.DAY_OF_MONTH, 1);
			to.set(GregorianCalendar.DAY_OF_MONTH, to.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		if (options.indexOf(option) == ++i) {
			from.set(GregorianCalendar.DAY_OF_MONTH, 1);
			//next month
			to.add(GregorianCalendar.MONTH, 1);
			to.set(GregorianCalendar.DAY_OF_MONTH, to.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		if (options.indexOf(option) == ++i) {
			from.set(GregorianCalendar.DAY_OF_MONTH, 1);
			//next two month
			to.add(GregorianCalendar.MONTH, 2);
			to.set(GregorianCalendar.DAY_OF_MONTH, to.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		if (options.indexOf(option) == ++i) {
			from.set(GregorianCalendar.DAY_OF_MONTH, 1);
			//next three month
			to.add(GregorianCalendar.MONTH, 3);
			to.set(GregorianCalendar.DAY_OF_MONTH, to.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		if (options.indexOf(option) == ++i) {
			GregorianCalendar monthYear;
			icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$
			JMonthYearChooser monthYearChooser = new JMonthYearChooser();
			int r = JOptionPane.showConfirmDialog(MedicalBrowser.this,
					monthYearChooser,
					MessageBundle.getMessage("angal.billbrowser.month"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					icon);

			if (r == JOptionPane.OK_OPTION) {
				monthYear = monthYearChooser.getDate();
			} else {
				return;
			}

			from.set(GregorianCalendar.DAY_OF_MONTH, 1);
			//other month
			to.set(GregorianCalendar.MONTH, monthYear.get(GregorianCalendar.MONTH));
			to.set(GregorianCalendar.YEAR, monthYear.get(GregorianCalendar.YEAR));
			to.set(GregorianCalendar.DAY_OF_MONTH, to.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		new GenericReportFromDateToDate(
				TimeTools.formatDateTime(from, "dd/MM/yyyy"),
				TimeTools.formatDateTime(to, "dd/MM/yyyy"),
				"PharmaceuticalExpiration",
				MessageBundle.getMessage("angal.medicals.expiringreport"),
				false);
	}

	class MedicalBrowsingModel extends DefaultTableModel {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		ArrayList<Medical> medicalList = new ArrayList<Medical>();

		public MedicalBrowsingModel(String key, boolean isType) {
			if (isType) {
				try {
					medicalList = pMedicals = medicalBrowsingManager.getMedicals(key, false);
				} catch (OHServiceException e) {
					pMedicals = null;
					OHServiceExceptionUtil.showMessages(e);
				}
			} else {
				for (Medical med : pMedicals) {
					if (key != null) {

						String s = key + lastKey;
						s = s.trim();
						String[] tokens = s.split(" ");

						if (!s.equals("")) {
							String description = med.getProd_code() + med.getDescription();
							int a = 0;
							for (int j = 0; j < tokens.length; j++) {
								String token = tokens[j].toLowerCase();
								if (description.toLowerCase().contains(token)) {
									a++;
								}
							}
							if (a == tokens.length)
								medicalList.add(med);
						} else
							medicalList.add(med);
					} else
						medicalList.add(med);
				}
			}
		}

		public MedicalBrowsingModel() {
			try {
				medicalList = pMedicals = medicalBrowsingManager.getMedicals(null, false);
			} catch (OHServiceException e) {
				pMedicals = null;
				OHServiceExceptionUtil.showMessages(e);
			}
		}

		public Class<?> getColumnClass(int c) {
			if (c == 0) {
				return String.class;
			} else if (c == 1) {
				return String.class;
			} else if (c == 2) {
				return String.class;
			} else if (c == 3) {
				return Integer.class;
			} else if (c == 4) {
				return Double.class;
			} else if (c == 5) {
				return Double.class;
			} else if (c == 6) {
				return Boolean.class;
			}
			return null;
		}

		public int getRowCount() {
			if (medicalList == null)
				return 0;
			return medicalList.size();
		}

		public String getColumnName(int c) {
			return pColums[c];
		}

		public int getColumnCount() {
			return pColums.length;
		}

		public Object getValueAt(int r, int c) {
			Medical med = medicalList.get(r);
			double actualQty = med.getInitialqty() + med.getInqty() - med.getOutqty();
			double minQuantity = med.getMinqty();
			if (c == -1) {
				return med;
			} else if (c == 0) {
				return med.getType().getDescription();
			} else if (c == 1) {
				return med.getProd_code();
			} else if (c == 2) {
				return med.getDescription();
			} else if (c == 3) {
				return med.getPcsperpck();
			} else if (c == 4) {
				return actualQty;
			} else if (c == 5) {
				return minQuantity;
			} else if (c == 6) {
				//if(actualQty<=minQuantity)return true;
				if (actualQty == 0)
					return true;
				else
					return false;
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}

	}

	class ColorTableCellRenderer extends DefaultTableCellRenderer {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			cell.setForeground(Color.BLACK);
			Medical med = pMedicals.get(row);
			double actualQty = med.getInitialqty() + med.getInqty() - med.getOutqty();
			if (((Boolean) table.getValueAt(row, 6)).booleanValue())
				cell.setForeground(Color.GRAY); // out of stock
			if (med.getMinqty() != 0 && actualQty <= med.getMinqty())
				cell.setForeground(Color.RED); // under critical level
			return cell;
		}
	}

}
