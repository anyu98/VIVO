/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.web.templatemodels.individual;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.vitro.webapp.beans.DataPropertyStatement;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.config.ConfigurationProperties;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder.ParamMap;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder.Route;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;

public class IndividualTemplateModel extends BaseIndividualTemplateModel {

    private static final Log log = LogFactory.getLog(IndividualTemplateModel.class);
    private Map<String, String> qrData = null;
    
    public IndividualTemplateModel(Individual individual, VitroRequest vreq) {
        super(individual, vreq);
    }
    
    private String getBaseVisUrl() {
        return getUrl(Route.VISUALIZATION.path(), "uri", getUri());
    }
    
    private String getVisUrl(ParamMap params) {
        String baseVisUrl = getBaseVisUrl();
        return UrlBuilder.addParams(baseVisUrl, params);
    }
    
    private String getVisUrl(String...params) {
        return getVisUrl(new ParamMap(params));
    }
    
    private String getPersonVisUrl(ParamMap params) {
        if (!isPerson()) {
            return null;
        }
        ParamMap paramMap = new ParamMap("vis", "person_level");
        paramMap.put(params);
        return getVisUrl(paramMap);
    }
    
      
    /* Access methods for templates */

    public boolean isPerson() {
        return isVClass("http://xmlns.com/foaf/0.1/Person");
    }
    
    public boolean isOrganization() {
        return isVClass("http://xmlns.com/foaf/0.1/Organization");        
    }
    
    public String getCoAuthorVisUrl() {
        return getPersonVisUrl(new ParamMap("vis_mode", "coauthor"));
    }
    
    public String getCoInvestigatorVisUrl() {
        return getPersonVisUrl(new ParamMap("vis_mode", "copi"));
    }

    public String getTemporalGraphUrl() {
        if (!isOrganization()) {
            return null;
        }
        return getVisUrl("vis", "entity_comparison");
    }

    public String getSelfEditingId() {
        String id = null;
        String idMatchingProperty = ConfigurationProperties.getBean(getServletContext()).getProperty("selfEditing.idMatchingProperty");
        if (! StringUtils.isBlank(idMatchingProperty)) {
            // Use assertions model to side-step filtering. We need to get the value regardless of whether the property
            // is visible to the current user.
            WebappDaoFactory wdf = vreq.getAssertionsWebappDaoFactory();
            Collection<DataPropertyStatement> ids = 
                wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, idMatchingProperty);
            if (ids.size() > 0) {
                id = ids.iterator().next().getData();
            }
        }
        return id;
    }
    
    public Map<String, String> getQrData() {
        if(qrData == null)
            qrData = generateQrData();
        return qrData;
    }

    private Map<String, String> generateQrData() {
        String core = "http://vivoweb.org/ontology/core#";
        String foaf = "http://xmlns.com/foaf/0.1/";

        Map<String,String> qrData = new HashMap<String,String>();
        WebappDaoFactory wdf = vreq.getAssertionsWebappDaoFactory();
        Collection<DataPropertyStatement> firstNames = wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, foaf + "firstName");
        Collection<DataPropertyStatement> lastNames = wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, foaf + "lastName");
        Collection<DataPropertyStatement> preferredTitles = wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, core + "preferredTitle");
        Collection<DataPropertyStatement> phoneNumbers = wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, core + "phoneNumber");
        Collection<DataPropertyStatement> emails = wdf.getDataPropertyStatementDao().getDataPropertyStatementsForIndividualByDataPropertyURI(individual, core + "email");

        if(firstNames.size() > 0)
            qrData.put("firstName", firstNames.toArray(new DataPropertyStatement[firstNames.size()])[0].getData());
        if(lastNames.size() > 0)
            qrData.put("lastName", lastNames.toArray(new DataPropertyStatement[firstNames.size()])[0].getData());
        if(preferredTitles.size() > 0)
            qrData.put("preferredTitle", preferredTitles.toArray(new DataPropertyStatement[firstNames.size()])[0].getData());
        if(phoneNumbers.size() > 0)
            qrData.put("phoneNumber", phoneNumbers.toArray(new DataPropertyStatement[firstNames.size()])[0].getData());
        if(emails.size() > 0)
            qrData.put("email", emails.toArray(new DataPropertyStatement[firstNames.size()])[0].getData());

        String tempUrl = vreq.getRequestURL().toString();
        String prefix = "http://";
        tempUrl = tempUrl.substring(0, tempUrl.replace(prefix, "").indexOf("/") + prefix.length());
        String profileUrl = getProfileUrl();
        String externalUrl = tempUrl + profileUrl;
        qrData.put("externalUrl", externalUrl);

        String individualUri = individual.getURI();
        String contextPath = vreq.getContextPath();
        qrData.put("exportQrCodeUrl", contextPath + "/qrcode?uri=" + UrlBuilder.urlEncode(individualUri));
        
        qrData.put("aboutQrCodesUrl", contextPath + "/qrcode/about");
        
        return qrData;
    }
}
