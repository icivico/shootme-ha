package com.iccapps.sip.ha.cache;

import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.AbstractHASipDialog;
import gov.nist.javax.sip.stack.ConfirmedReplicationSipDialog;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.header.ContactHeader;

import org.apache.log4j.Logger;
import org.mobicents.ha.javax.sip.ClusteredSipStack;
import org.mobicents.ha.javax.sip.HASipDialog;
import org.mobicents.ha.javax.sip.HASipDialogFactory;
import org.mobicents.ha.javax.sip.cache.SipCache;
import org.mobicents.ha.javax.sip.cache.SipCacheException;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HazelcastCache implements SipCache {
	
	public static HazelcastInstance hz;
	private ClusteredSipStack stack;
	private Logger logger = Logger.getLogger(HazelcastCache.class);
	private IMap<String, Object> dialogs;
	private IMap<String, Object> appDataMap;
	
	@Override
	public void evictDialog(String arg0) {
		logger.debug("evictDialog");
		try {
			removeDialog(arg0);
			
		} catch (SipCacheException e) {
			e.printStackTrace();
		}
	}

	@Override
	public SIPClientTransaction getClientTransaction(String arg0) throws SipCacheException {
		logger.debug("getClientTX(" + arg0 + ")");
		return null;
	}

	@Override
	public SIPDialog getDialog(String arg0) throws SipCacheException {
		logger.debug("getDialog("+ arg0 +")");
		Object metaData = dialogs.get(arg0);
		Object appData = appDataMap.get(arg0);
		if (metaData != null) {
			SIPDialog d = (SIPDialog) createDialog(arg0, (Map<String, Object>) metaData, appData);
			return d;
			
		} else {
			return null;
		}
	}

	@Override
	public SIPServerTransaction getServerTransaction(String arg0) throws SipCacheException {
		logger.debug("getServerTX(" + arg0 + ")");
		return null;
	}

	@Override
	public boolean inLocalMode() {
		return false;
	}

	@Override
	public void init() throws SipCacheException {
		Config cfg = new Config();
		cfg.setProperty("hazelcast.logging.type", "log4j");
        hz = Hazelcast.newHazelcastInstance(cfg);
        dialogs = hz.getMap("dialogs");
        appDataMap = hz.getMap("apps");
	}

	@Override
	public void putClientTransaction(SIPClientTransaction arg0) throws SipCacheException {
		logger.debug("putServerTX(" + arg0 + ")");
	}

	@Override
	public void putDialog(SIPDialog arg0) throws SipCacheException {
		logger.debug("putDialog(" + arg0.getDialogId() + ")");
		if (arg0 instanceof ConfirmedReplicationSipDialog) {
			Object o = ((ConfirmedReplicationSipDialog)arg0).getMetaDataToReplicate(); 
			dialogs.put(arg0.getDialogId(), o);
			Object app = ((ConfirmedReplicationSipDialog) arg0).getApplicationDataToReplicate();
			if (app != null)
				appDataMap.put(arg0.getDialogId(), app);
		}
	}

	@Override
	public void putServerTransaction(SIPServerTransaction arg0) throws SipCacheException {
		logger.debug("putServerTX(" + arg0 + ")");
		
	}

	@Override
	public void removeClientTransaction(String arg0) throws SipCacheException {
		logger.debug("removeClientTX(" + arg0 + ")");
	}

	@Override
	public void removeDialog(String arg0) throws SipCacheException {
		logger.debug("removeDialog(" + arg0 + ")");
		dialogs.remove(arg0);
	}

	@Override
	public void removeServerTransaction(String arg0) throws SipCacheException {
		logger.debug("removeServerTX(" + arg0 + ")");
	}

	@Override
	public void setClusteredSipStack(ClusteredSipStack arg0) {
		stack = arg0;
	}

	@Override
	public void setConfigurationProperties(Properties arg0) {
		
	}

	@Override
	public void start() throws SipCacheException {
		
	}

	@Override
	public void stop() throws SipCacheException {
		
	}

	@Override
	public void updateDialog(SIPDialog arg0) throws SipCacheException {
		logger.debug("updateDialog(" + arg0.getDialogId() + ")");
		if (arg0 instanceof ConfirmedReplicationSipDialog) {
			Object o = ((ConfirmedReplicationSipDialog)arg0).getMetaDataToReplicate(); 
			dialogs.put(arg0.getDialogId(), o);
		}
	}
	
	public HASipDialog createDialog(String dialogId, Map<String, Object> dialogMetaData, Object dialogAppData) throws SipCacheException {
		HASipDialog haSipDialog = null; 
		if(dialogMetaData != null) {
			logger.debug("sipStack " + this + " dialog " + dialogId + " is present in the distributed cache, recreating it locally");
			final String lastResponseStringified = (String) dialogMetaData.get(AbstractHASipDialog.LAST_RESPONSE);
			try {
				final SIPResponse lastResponse = (SIPResponse) SipFactory.getInstance().createMessageFactory().createResponse(lastResponseStringified);
				haSipDialog = HASipDialogFactory.createHASipDialog(stack.getReplicationStrategy(), (SipProviderImpl)stack.getSipProviders().next(), lastResponse);
				haSipDialog.setDialogId(dialogId);
				updateDialogMetaData(dialogMetaData, dialogAppData, haSipDialog, true);
				// setLastResponse won't be called on recreation since version will be null on recreation			
				haSipDialog.setLastResponse(lastResponse);				
				logger.debug("HA SIP Dialog " + dialogId + " localTag  = " + haSipDialog.getLocalTag());
				logger.debug("HA SIP Dialog " + dialogId + " remoteTag  = " + haSipDialog.getRemoteTag());
				logger.debug("HA SIP Dialog " + dialogId + " localParty = " + haSipDialog.getLocalParty());
				logger.debug("HA SIP Dialog " + dialogId + " remoteParty  = " + haSipDialog.getRemoteParty());
				
			} catch (PeerUnavailableException e) {
				throw new SipCacheException("A problem occured while retrieving the following dialog " + dialogId + " from the Cache", e);
			} catch (ParseException e) {
				throw new SipCacheException("A problem occured while retrieving the following dialog " + dialogId + " from the Cache", e);
			}
		}
		
		return haSipDialog;
	}
	
	
	/**
	 * Update the haSipDialog passed in param with the dialogMetaData and app meta data
	 * @param dialogMetaData
	 * @param dialogAppData
	 * @param haSipDialog
	 * @throws ParseException
	 * @throws PeerUnavailableException
	 */
	private void updateDialogMetaData(Map<String, Object> dialogMetaData, Object dialogAppData, HASipDialog haSipDialog, boolean recreation) 
			throws ParseException, PeerUnavailableException {
		haSipDialog.setMetaDataToReplicate(dialogMetaData, recreation);
		haSipDialog.setApplicationDataToReplicate(dialogAppData);
		final String contactStringified = (String) dialogMetaData.get(AbstractHASipDialog.CONTACT_HEADER);
		logger.debug("contactStringified " + contactStringified);
		
		if(contactStringified != null) {
			Address contactAddress = SipFactory.getInstance().createAddressFactory().createAddress(contactStringified);
			ContactHeader contactHeader = SipFactory.getInstance().createHeaderFactory().createContactHeader(contactAddress);
			logger.debug("contactHeader " + contactHeader);
			logger.debug("contactURI " + contactHeader.getAddress().getURI());
			haSipDialog.setContactHeader(contactHeader);
		}
	}	
}
