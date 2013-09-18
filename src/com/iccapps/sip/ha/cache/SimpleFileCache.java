package com.iccapps.sip.ha.cache;


import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.AbstractHASipDialog;
import gov.nist.javax.sip.stack.ConfirmedReplicationSipDialog;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.HashMap;
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

public class SimpleFileCache implements SipCache {
	
	//private Cache cache;
	private ClusteredSipStack stack;
	private Logger logger = Logger.getLogger(SimpleFileCache.class);
	private HashMap<String, Object> dialogs = new HashMap<String, Object>();

	@Override
	public void evictDialog(String arg0) {
		logger.debug("evictDialog");
		
	}

	@Override
	public SIPClientTransaction getClientTransaction(String arg0)
			throws SipCacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SIPDialog getDialog(String arg0) throws SipCacheException {
		logger.debug("************************ getDialog("+ arg0 +") ****************************");
		/*Element e = cache.get(arg0);
		if (e != null) {
			logger.debug("********* Found " + arg0 + " ********************");
			ConfirmedReplicationSipDialog d = new ConfirmedReplicationSipDialog(null);
			d.initAfterLoad(stack);
			d.setMetaDataToReplicate((Map<String, Object>)e.getValue(), true);
			return d;
			
		} else
			return null;*/
		/*Object o  = dialogs.get(arg0);
		if (o != null) {
			logger.debug("********* Found " + arg0 + " ********************");
			ConfirmedReplicationSipDialog d = new ConfirmedReplicationSipDialog(null);
			d.initAfterLoad(stack);
			d.setMetaDataToReplicate((Map<String, Object>)o, true);
			return d;
			
		} else {
			return null;
		}*/
		Object metaData = dialogs.get(arg0);
		if (metaData != null) {
			SIPDialog d = (SIPDialog) createDialog(arg0, (Map<String, Object>) metaData, null);
			return d;
			
		} else {
			return null;
		}
	}

	@Override
	public SIPServerTransaction getServerTransaction(String arg0)
			throws SipCacheException {
		logger.debug("************************ getServerTX(" + arg0 + ") ****************************");
		
		return null;
	}

	@Override
	public boolean inLocalMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void init() throws SipCacheException {
		//CacheManager.getInstance().addCache("sip");
		//cache = CacheManager.getInstance().getCache("sip");ç
		recover();
	}

	@Override
	public void putClientTransaction(SIPClientTransaction arg0)
			throws SipCacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putDialog(SIPDialog arg0) throws SipCacheException {
		logger.debug("************************ putDialog(" + arg0.getDialogId() + ") ****************************");
		if (arg0 instanceof ConfirmedReplicationSipDialog) {
			Object o = ((ConfirmedReplicationSipDialog)arg0).getMetaDataToReplicate(); 
			//cache.put(new Element(arg0.getDialogId(), o));
			dialogs.put(arg0.getDialogId(), o);
			persist();
		}
	}

	@Override
	public void putServerTransaction(SIPServerTransaction arg0)
			throws SipCacheException {
		logger.debug("************************ putServerTX(" + arg0.getClass().getName() + ") ****************************");
		
	}

	@Override
	public void removeClientTransaction(String arg0) throws SipCacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDialog(String arg0) throws SipCacheException {
		System.out.println("************************ removeDialog(" + arg0 + ") ****************************");
		//cache.remove(arg0);
		dialogs.remove(arg0);
		persist();
	}

	@Override
	public void removeServerTransaction(String arg0) throws SipCacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClusteredSipStack(ClusteredSipStack arg0) {
		stack = arg0;
		
	}

	@Override
	public void setConfigurationProperties(Properties arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws SipCacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws SipCacheException {
		
	}

	@Override
	public void updateDialog(SIPDialog arg0) throws SipCacheException {
		System.out.println("************************ updateDialog(" + arg0.getDialogId() + ") ****************************");
		if (arg0 instanceof ConfirmedReplicationSipDialog) {
			Object o = ((ConfirmedReplicationSipDialog)arg0).getMetaDataToReplicate(); 
			//cache.put(new Element(arg0.getDialogId(), o));
			dialogs.put(arg0.getDialogId(), o);
			persist();
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
	
	private synchronized void persist() {
		FileOutputStream fout;
		try {
			fout = new FileOutputStream("cache");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(dialogs);
			oos.close();
			fout.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void recover() {
		try {
			FileInputStream fin = new FileInputStream("cache");
			ObjectInputStream ois = new ObjectInputStream(fin);
			dialogs = (HashMap<String, Object>) ois.readObject();
			ois.close();
			fin.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
