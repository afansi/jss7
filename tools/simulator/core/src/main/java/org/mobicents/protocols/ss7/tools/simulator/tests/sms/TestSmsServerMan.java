/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012.
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.tools.simulator.tests.sms;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.MAPDialog;
import org.mobicents.protocols.ss7.map.api.MAPDialogListener;
import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.InformServiceCentreRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.LocationInfoWithLMSI;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPServiceSmsListener;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MoForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.ReportSMDeliveryStatusResponse;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMRequest;
import org.mobicents.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.mobicents.protocols.ss7.map.api.smstpdu.NumberingPlanIdentification;
import org.mobicents.protocols.ss7.map.api.smstpdu.TypeOfNumber;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tools.simulator.Stoppable;
import org.mobicents.protocols.ss7.tools.simulator.common.AddressNatureType;
import org.mobicents.protocols.ss7.tools.simulator.common.MapProtocolVersion;
import org.mobicents.protocols.ss7.tools.simulator.common.NumberingPlanType;
import org.mobicents.protocols.ss7.tools.simulator.common.TesterBase;
import org.mobicents.protocols.ss7.tools.simulator.level3.MapMan;
import org.mobicents.protocols.ss7.tools.simulator.management.TesterHost;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class TestSmsServerMan extends TesterBase implements TestSmsServerManMBean, Stoppable, MAPDialogListener, MAPServiceSmsListener {

	public static String SOURCE_NAME = "TestSmsServer";

	private static final String ADDRESS_NATURE = "addressNature";
	private static final String NUMBERING_PLAN = "numberingPlan";
	private static final String MAP_PROTOCOL_VERSION = "mapProtocolVersion";
	private static final String HLR_SSN = "hlrSsn";
	private static final String VLR_SSN = "vlrSsn";
	private static final String TYPE_OF_NUMBER = "typeOfNumber";
	private static final String NUMBERING_PLAN_IDENTIFICATION = "numberingPlanIdentification";

	private AddressNature addressNature = AddressNature.international_number;
	private NumberingPlan numberingPlan = NumberingPlan.ISDN;
	private MapProtocolVersion mapProtocolVersion = new MapProtocolVersion(MapProtocolVersion.VAL_MAP_V3);
	private int hlrSsn = 6;
	private int vlrSsn = 8;
	private TypeOfNumber typeOfNumber = TypeOfNumber.InternationalNumber;
	private NumberingPlanIdentification numberingPlanIdentification = NumberingPlanIdentification.ISDNTelephoneNumberingPlan;

	private final String name;
	private MapMan mapMan;

	private boolean isStarted = false;
	private int countSriReq = 0;
	private int countSriResp = 0;
	private int countMtFsmReq = 0;
	private int countMtFsmResp = 0;
	private int countMoFsmReq = 0;
	private int countMoFsmResp = 0;
	private String currentRequestDef = "";
	private boolean needSendSend = false;
	private boolean needSendClose = false;


	public TestSmsServerMan() {
		super(SOURCE_NAME);
		this.name = "???";
	}

	public TestSmsServerMan(String name) {
		super(SOURCE_NAME);
		this.name = name;
	}

	public void setTesterHost(TesterHost testerHost) {
		this.testerHost = testerHost;
	}

	public void setMapMan(MapMan val) {
		this.mapMan = val;
	}	


	@Override
	public AddressNatureType getAddressNature() {
		return new AddressNatureType(addressNature.getIndicator());
	}

	@Override
	public String getAddressNature_Value() {
		return addressNature.toString();
	}

	@Override
	public void setAddressNature(AddressNatureType val) {
		addressNature = AddressNature.getInstance(val.intValue());
		this.testerHost.markStore();
	}

	@Override
	public NumberingPlanType getNumberingPlan() {
		return new NumberingPlanType(numberingPlan.getIndicator());
	}

	@Override
	public String getNumberingPlan_Value() {
		return numberingPlan.toString();
	}

	@Override
	public void setNumberingPlan(NumberingPlanType val) {
		numberingPlan = NumberingPlan.getInstance(val.intValue());
		this.testerHost.markStore();
	}

	@Override
	public MapProtocolVersion getMapProtocolVersion() {
		return mapProtocolVersion;
	}

	@Override
	public String getMapProtocolVersion_Value() {
		return mapProtocolVersion.toString();
	}

	@Override
	public void setMapProtocolVersion(MapProtocolVersion val) {
		mapProtocolVersion = val;
		this.testerHost.markStore();
	}

	@Override
	public int getHlrSsn() {
		return hlrSsn;
	}

	@Override
	public void setHlrSsn(int val) {
		hlrSsn = val;
		this.testerHost.markStore();
	}

	@Override
	public int getVlrSsn() {
		return vlrSsn;
	}

	@Override
	public void setVlrSsn(int val) {
		vlrSsn = val;
		this.testerHost.markStore();
	}

	@Override
	public TypeOfNumberType getTypeOfNumber() {
		return new TypeOfNumberType(typeOfNumber.getCode());
	}

	@Override
	public String getTypeOfNumber_Value() {
		return typeOfNumber.toString();
	}

	@Override
	public void setTypeOfNumber(TypeOfNumberType val) {
		typeOfNumber = TypeOfNumber.getInstance(val.intValue());
		this.testerHost.markStore();
	}

	@Override
	public NumberingPlanIdentificationType getNumberingPlanIdentification() {
		return new NumberingPlanIdentificationType(numberingPlanIdentification.getCode());
	}

	@Override
	public String getNumberingPlanIdentification_Value() {
		return numberingPlanIdentification.toString();
	}

	@Override
	public void setNumberingPlanIdentification(NumberingPlanIdentificationType val) {
		numberingPlanIdentification = NumberingPlanIdentification.getInstance(val.intValue());
		this.testerHost.markStore();
	}


	@Override
	public void putAddressNature(String val) {
		AddressNatureType x = AddressNatureType.createInstance(val);
		if (x != null)
			this.setAddressNature(x);
	}

	@Override
	public void putNumberingPlan(String val) {
		NumberingPlanType x = NumberingPlanType.createInstance(val);
		if (x != null)
			this.setNumberingPlan(x);
	}

	@Override
	public void putMapProtocolVersion(String val) {
		MapProtocolVersion x = MapProtocolVersion.createInstance(val);
		if (x != null)
			this.setMapProtocolVersion(x);
	}

	@Override
	public void putTypeOfNumber(String val) {
		TypeOfNumberType x = TypeOfNumberType.createInstance(val);
		if (x != null)
			this.setTypeOfNumber(x);
	}

	@Override
	public void putNumberingPlanIdentification(String val) {
		NumberingPlanIdentificationType x = NumberingPlanIdentificationType.createInstance(val);
		if (x != null)
			this.setNumberingPlanIdentification(x);
	}

	protected static final XMLFormat<TestSmsServerMan> XML = new XMLFormat<TestSmsServerMan>(TestSmsServerMan.class) {

		public void write(TestSmsServerMan srv, OutputElement xml) throws XMLStreamException {
			xml.setAttribute(HLR_SSN, srv.hlrSsn);
			xml.setAttribute(VLR_SSN, srv.vlrSsn);

			xml.add(srv.addressNature.toString(), ADDRESS_NATURE);
			xml.add(srv.numberingPlan.toString(), NUMBERING_PLAN);
			xml.add(srv.mapProtocolVersion.toString(), MAP_PROTOCOL_VERSION);
			xml.add(srv.typeOfNumber.toString(), TYPE_OF_NUMBER);
			xml.add(srv.numberingPlanIdentification.toString(), NUMBERING_PLAN_IDENTIFICATION);
		}

		public void read(InputElement xml, TestSmsServerMan srv) throws XMLStreamException {
			srv.hlrSsn = xml.getAttribute(HLR_SSN).toInt();
			srv.vlrSsn = xml.getAttribute(VLR_SSN).toInt();
			
			String an = (String) xml.get(ADDRESS_NATURE, String.class);
			srv.addressNature = AddressNature.valueOf(an);
			String np = (String) xml.get(NUMBERING_PLAN, String.class);
			srv.numberingPlan = NumberingPlan.valueOf(np);
			String mpv = (String) xml.get(MAP_PROTOCOL_VERSION, String.class);
			srv.mapProtocolVersion = MapProtocolVersion.createInstance(mpv);
			String ton = (String) xml.get(TYPE_OF_NUMBER, String.class);
			srv.typeOfNumber = TypeOfNumber.valueOf(ton);
			String npi = (String) xml.get(NUMBERING_PLAN_IDENTIFICATION, String.class);
			srv.numberingPlanIdentification = NumberingPlanIdentification.valueOf(npi);
		}
	};


	@Override
	public String getCurrentRequestDef() {
		return "LastDialog: " + currentRequestDef;
	}

	@Override
	public String getState() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(SOURCE_NAME);
		sb.append(": ");
		sb.append("<br>Count: countSriReq-");
		sb.append(countSriReq);
		sb.append(", countSriResp-");
		sb.append(countSriResp);
		sb.append("<br>countMtFsmReq-");
		sb.append(countMtFsmReq);
		sb.append(", countMtFsmResp-");
		sb.append(countMtFsmResp);
		sb.append("<br> countMoFsmReq-");
		sb.append(countMoFsmReq);
		sb.append(", countMoFsmResp-");
		sb.append(countMoFsmResp);
		sb.append("</html>");
		return sb.toString();
	}

	public boolean start() {
		MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
		mapProvider.getMAPServiceSms().acivate();
		mapProvider.getMAPServiceSms().addMAPServiceListener(this);
		mapProvider.addMAPDialogListener(this);
		this.testerHost.sendNotif(SOURCE_NAME, "SMS Server has been started", "", true);
		isStarted = true;

		return true;
	}

	@Override
	public void stop() {
		MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();
		isStarted = false;
		mapProvider.getMAPServiceSms().deactivate();
		mapProvider.getMAPServiceSms().removeMAPServiceListener(this);
		mapProvider.removeMAPDialogListener(this);
		this.testerHost.sendNotif(SOURCE_NAME, "SMS Server has been stopped", "", true);
	}

	@Override
	public void execute() {
	}


	@Override
	public String closeCurrentDialog() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String performSRIForSM(String destIsdnNumber) {
		if (!isStarted)
			return "The tester is not started";
		if (destIsdnNumber == null || destIsdnNumber.equals(""))
			return "DestIsdnNumber is empty";

		currentRequestDef = "";

		SccpAddress sa = this.mapMan.createOrigAddress();
		if (sa.getGlobalTitle() == null)
			return "Sccp ROUTING_ON_GLOBAL_TYTLE mode is needed";
		String serviceCentreAddr = sa.getGlobalTitle().getDigits();
		
		return doSendSri(destIsdnNumber, serviceCentreAddr, null);
	}

	private String doSendSri(String destIsdnNumber, String serviceCentreAddr, MoMessageData messageData) {

		MAPProvider mapProvider = this.mapMan.getMAPStack().getMAPProvider();

		MAPApplicationContextVersion vers;
		switch(this.mapProtocolVersion.intValue()){
		case MapProtocolVersion.VAL_MAP_V1:
			vers = MAPApplicationContextVersion.version1;
			break;
		case MapProtocolVersion.VAL_MAP_V2:
			vers = MAPApplicationContextVersion.version2;
			break;
		default:
			vers = MAPApplicationContextVersion.version3;
			break;
		}
		MAPApplicationContext mapAppContext = MAPApplicationContext.getInstance(MAPApplicationContextName.shortMsgGatewayContext, vers);
		
		ISDNAddressString msisdn = mapProvider.getMAPParameterFactory().createISDNAddressString(this.addressNature, this.numberingPlan, destIsdnNumber);
		AddressString serviceCentreAddress = mapProvider.getMAPParameterFactory().createAddressString(this.addressNature, this.numberingPlan, serviceCentreAddr);

		try {
			MAPDialogSms curDialog = mapProvider.getMAPServiceSms().createNewDialog(mapAppContext, this.mapMan.createOrigAddress(), null,
					this.mapMan.createDestAddress(destIsdnNumber, this.hlrSsn), null);
			curDialog.setUserObject(messageData);

			curDialog.addSendRoutingInfoForSMRequest(msisdn, true, serviceCentreAddress, null, false, null, null);
			curDialog.send();

			String sriData = createSriData(curDialog.getDialogId(), destIsdnNumber, serviceCentreAddr);
			currentRequestDef += "Sent SriReq;";
			this.countSriReq++;
			this.testerHost.sendNotif(SOURCE_NAME, "Sent: sriReq", sriData, true);

			return "SendRoutingInfoForSMRequest has been sent";
		} catch (MAPException ex) {
			return "Exception when sending SendRoutingInfoForSMRequest: " + ex.toString();
		}
	}

	private String createSriData(long dialogId, String destIsdnNumber, String serviceCentreAddr){
		StringBuilder sb = new StringBuilder();
		sb.append("dialogId=");
		sb.append(dialogId);
		sb.append(", destIsdnNumber=\"");
		sb.append(destIsdnNumber);
		sb.append("\", serviceCentreAddr=\"");
		sb.append(serviceCentreAddr);
		sb.append("\"");
		return sb.toString();
	}
	
	@Override
	public String performSRIForSM_MtForwardSM(String msg, String destIsdnNumber, String origIsdnNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String performMtForwardSM(String msg, String destImsi, String vlrNumber, String origIsdnNumber) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void onForwardShortMessageRequest(ForwardShortMessageRequest forwSmInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onForwardShortMessageResponse(ForwardShortMessageResponse forwSmRespInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoForwardShortMessageRequest(MoForwardShortMessageRequest moForwSmInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoForwardShortMessageResponse(MoForwardShortMessageResponse moForwSmRespInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMtForwardShortMessageRequest(MtForwardShortMessageRequest mtForwSmInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMtForwardShortMessageResponse(MtForwardShortMessageResponse mtForwSmRespInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendRoutingInfoForSMRequest(SendRoutingInfoForSMRequest sendRoutingInfoForSMInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendRoutingInfoForSMResponse(SendRoutingInfoForSMResponse ind) {
		if (!isStarted)
			return;

		this.countSriResp++;

		MAPDialogSms curDialog = ind.getMAPDialog();
		long invokeId = curDialog.getDialogId();
		LocationInfoWithLMSI li = ind.getLocationInfoWithLMSI();
		String vlrNum = "";
		if (li != null && li.getNetworkNodeNumber() != null)
			vlrNum = li.getNetworkNodeNumber().getAddress();
		currentRequestDef += "Rsvd SriResp;";
		String uData = this.createSriRespData(invokeId, ind.getIMSI().getData(), vlrNum);
		this.testerHost.sendNotif(SOURCE_NAME, "Rcvd: sriReq", uData, true);

		if (curDialog.getUserObject() != null && vlrNum != null && !vlrNum.equals("")) {
			// TODO Auto-generated method stub
			// add sending mo-fmr
			// ............................
		}
	}

	private String createSriRespData(long dialogId, String imsi, String vlrNumber) {
		StringBuilder sb = new StringBuilder();
		sb.append("dialogId=");
		sb.append(dialogId);
		sb.append(", imsi=\"");
		sb.append(imsi);
		sb.append("\", vlrNumber=\"");
		sb.append(vlrNumber);
		sb.append("\"");
		return sb.toString();
	}

	@Override
	public void onReportSMDeliveryStatusRequest(ReportSMDeliveryStatusRequest reportSMDeliveryStatusInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReportSMDeliveryStatusResponse(ReportSMDeliveryStatusResponse reportSMDeliveryStatusRespInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInformServiceCentreRequest(InformServiceCentreRequest informServiceCentreInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAlertServiceCentreRequest(AlertServiceCentreRequest alertServiceCentreInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAlertServiceCentreResponse(AlertServiceCentreResponse alertServiceCentreInd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialogDelimiter(MAPDialog mapDialog) {
		try {
			if (needSendSend) {
				needSendSend = false;
				mapDialog.send();
			}
		} catch (Exception e) {
			this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking send() : " + e.getMessage(), e, true);
		}
		try {
			if (needSendClose) {
				needSendClose = false;
				mapDialog.close(false);
			}
		} catch (Exception e) {
			this.testerHost.sendNotif(SOURCE_NAME, "Exception when invoking close() : " + e.getMessage(), e, true);
		}
	}

	private class MoMessageData {
		public String msg;
		public String origIsdnNumber;
	}
}
