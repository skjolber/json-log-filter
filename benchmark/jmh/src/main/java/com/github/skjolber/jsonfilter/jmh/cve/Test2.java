package com.github.skjolber.jsonfilter.jmh.cve;

import com.github.skjolber.jsonfilter.base.AbstractPathJsonFilter;
import com.github.skjolber.jsonfilter.core.SingleAnyPathJsonFilter;
import dev.blaauwendraad.masker.json.JsonMasker;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Test2 {

    private static String json = "{\"CVE_data_type\":\"CVE\",\"CVE_data_format\":\"MITRE\",\"CVE_data_version\":\"4.0\",\"CVE_data_numberOfCVEs\":\"7128\",\"CVE_data_timestamp\":\"2019-10-11T08:31Z\",\"CVE_Items\":[{\"cve\":{\"data_type\":\"CVE\",\"data_format\":\"MITRE\",\"data_version\":\"4.0\",\"CVE_data_meta\":{\"ID\":\"CVE-2006-0001\",\"ASSIGNER\":\"cve@mitre.org\"},\"affects\":{\"vendor\":{\"vendor_data\":[{\"vendor_name\":\"microsoft\",\"product\":{\"product_data\":[{\"product_name\":\"office\",\"version\":{\"version_data\":[{\"version_value\":\"2000\",\"version_affected\":\"=\"},{\"version_value\":\"2003\",\"version_affected\":\"=\"},{\"version_value\":\"xp\",\"version_affected\":\"=\"}]}},{\"product_name\":\"publisher\",\"version\":{\"version_data\":[{\"version_value\":\"2000\",\"version_affected\":\"=\"},{\"version_value\":\"2002\",\"version_affected\":\"=\"},{\"version_value\":\"2003\",\"version_affected\":\"=\"}]}}]}}]}},\"problemtype\":{\"problemtype_data\":[{\"description\":[{\"lang\":\"en\",\"value\":\"CWE-119\"}]}]},\"references\":{\"reference_data\":[{\"url\":\"http://secunia.com/advisories/21863\",\"name\":\"21863\",\"refsource\":\"SECUNIA\",\"tags\":[\"Patch\",\"Vendor Advisory\"]},{\"url\":\"http://securityreason.com/securityalert/1548\",\"name\":\"1548\",\"refsource\":\"SREASON\",\"tags\":[]},{\"url\":\"http://securitytracker.com/id?1016825\",\"name\":\"1016825\",\"refsource\":\"SECTRACK\",\"tags\":[]},{\"url\":\"http://www.computerterrorism.com/research/ct12-09-2006-2.htm\",\"name\":\"http://www.computerterrorism.com/research/ct12-09-2006-2.htm\",\"refsource\":\"MISC\",\"tags\":[\"Exploit\",\"Patch\",\"Vendor Advisory\"]},{\"url\":\"http://www.kb.cert.org/vuls/id/406236\",\"name\":\"VU#406236\",\"refsource\":\"CERT-VN\",\"tags\":[\"US Government Resource\"]},{\"url\":\"http://www.securityfocus.com/archive/1/445824/100/0/threaded\",\"name\":\"20060912 Computer Terrorism (UK) :: Incident Response Centre - Microsoft Publisher Font Parsing Vulnerability\",\"refsource\":\"BUGTRAQ\",\"tags\":[]},{\"url\":\"http://www.securityfocus.com/archive/1/446630/100/100/threaded\",\"name\":\"SSRT061187\",\"refsource\":\"HP\",\"tags\":[]},{\"url\":\"http://www.securityfocus.com/bid/19951\",\"name\":\"19951\",\"refsource\":\"BID\",\"tags\":[\"Patch\"]},{\"url\":\"http://www.us-cert.gov/cas/techalerts/TA06-255A.html\",\"name\":\"TA06-255A\",\"refsource\":\"CERT\",\"tags\":[\"US Government Resource\"]},{\"url\":\"http://www.vupen.com/english/advisories/2006/3565\",\"name\":\"ADV-2006-3565\",\"refsource\":\"VUPEN\",\"tags\":[]},{\"url\":\"https://docs.microsoft.com/en-us/security-updates/securitybulletins/2006/ms06-054\",\"name\":\"MS06-054\",\"refsource\":\"MS\",\"tags\":[]},{\"url\":\"https://exchange.xforce.ibmcloud.com/vulnerabilities/28648\",\"name\":\"publisher-pub-code-execution(28648)\",\"refsource\":\"XF\",\"tags\":[]},{\"url\":\"https://oval.cisecurity.org/repository/search/definition/oval%3Aorg.mitre.oval%3Adef%3A590\",\"name\":\"oval:org.mitre.oval:def:590\",\"refsource\":\"OVAL\",\"tags\":[]}]},\"description\":{\"description_data\":[{\"lang\":\"en\",\"value\":\"Stack-based buffer overflow in Microsoft Publisher 2000 through 2003 allows user-assisted remote attackers to execute arbitrary code via a crafted PUB file, which causes an overflow when parsing fonts.\"}]}}}]}";

    public static final void main(String[] args) {

        SingleAnyPathJsonFilter filter = new SingleAnyPathJsonFilter(-1, "//product_name", AbstractPathJsonFilter.FilterType.ANON);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        for(long i = 0; i < 1; i++) {
            byte[] process = filter.process(bytes);

            if (process.length < 128) {
                throw new RuntimeException();
            }
            System.out.println(new String(process));
        }

    }

}
