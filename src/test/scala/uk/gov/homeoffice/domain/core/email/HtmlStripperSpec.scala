package uk.gov.homeoffice.domain.core.email

import org.specs2.mutable.Specification

class HtmlStripperSpec extends Specification {

  "HtmlStripper" should {
    "strip HTML" in {
      val body = HtmlStripper.bodyText(htmlEmail)

      body mustEqual ("""| 
         |
         |Dear John Smith,
         |
         |Membership number: RTDK8ZPNP
         |
         |Apply to update your Registered Traveller membership at
         |http/update-passport/details/A_Update_Token
         |
         |This link will expire within 24 hours. There is a non-refundable £20 charge for
         |changing your passport details.
         |
         |
         |--------------------------------------------------------------
         |Contact us
         |--------------------------------------------------------------
         |Contact us at RTinbox@homeoffice.gsi.gov.uk if you are unable to access this
         |link or have any questions. Please include your membership number.
         |
         |We usually reply to enquiries within 10 working days.
         |
         | 
         |
         |Yours sincerely,
         |
         | 
         |
         |Registered Traveller Team
         |
         | 
         |
         |
         |--------------------------------------------------------------
         |--------------------------------------------------------------
         |This email and any attachments transmitted with it are intended for the named
         |recipient only. If you receive this email in error please notify us immediately
         |and then delete the email and any attachments. It is the responsibility of the
         |recipient to check for the presence of viruses before opening any attachments.
         |
         |
         |""".stripMargin)
    }
  }

  val htmlEmail = """
                    |<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                    |<html xmlns="http://www.w3.org/1999/xhtml">
                    |<head>
                    |	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                    |	<title>Registered Traveller - update your membership</title>
                    |	<style type="text/css">
                    |
                    |
                    |		#outlook a {padding:0;} /* Force Outlook to provide a "view in browser" menu link. */
                    |		html body.rtemail{width:100% !important; -webkit-text-size-adjust:100%; -ms-text-size-adjust:100%; margin:0; padding:0;}
                    |		/* Prevent Webkit and Windows Mobile platforms from changing default font sizes.*/
                    |		.ExternalClass {width:100%;} /* Force Hotmail to display emails at full width */
                    |		.ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div {line-height: 100%;}
                    |		/* Forces Hotmail to display normal line spacing.  More on that: http://www.emailonaimmigration.com/forum/viewthread/43/ */
                    |		#backgroundTable {margin:0; padding:0; width:100% !important; line-height: 100% !important;}
                    |		/* End reset */
                    |
                    |		/* Some sensible defaults for images
                    |		Bring inline: Yes. */
                    |		html .rtemail img {outline:none; text-decoration:none; -ms-interpolation-mode: bicubic;}
                    |		html.rtemail a img {border:none;}
                    |		.image_fix {display:block;}
                    |
                    |		/* Yahoo paragraph fix
                    |		Bring inline: Yes. */
                    |		html .rtemail p {margin: 1em 0;}
                    |
                    |		/* Hotmail header color reset
                    |		Bring inline: Yes. */
                    |		html .rtemail h1,
                    |		html .rtemail h2,
                    |		html .rtemail h3,
                    |		html .rtemail h4,
                    |		html .rtemail h5,
                    |		html .rtemail h6 {
                    |            font-family: 'Helvetic Neue', sans-serif, arial;
                    |            color: black !important;
                    |        }
                    |
                    |		html .rtemail h1 a,
                    |		html .rtemail h2 a,
                    |		html .rtemail h3 a,
                    |		html .rtemail h4 a,
                    |		html .rtemail h5 a,
                    |		html .rtemail h6 a {color: blue !important;}
                    |
                    |		html .rtemail h1 a:active,
                    |		html .rtemail h2 a:active,
                    |		html .rtemail h3 a:active,
                    |		html .rtemail h4 a:active,
                    |		html .rtemail h5 a:active,
                    |		html .rtemail h6 a:active {
                    |		color: red !important; /* Preferably not the same color as the normal header link color.  There is limited support for psuedo classes in email clients, this was added just for good measure. */
                    |		}
                    |
                    |		html .rtemail h1 a:visited,
                    |		html .rtemail h2 a:visited,
                    |		html .rtemail h3 a:visited,
                    |		html .rtemail h4 a:visited,
                    |		html .rtemail h5 a:visited,
                    |		html .rtemail h6 a:visited {
                    |		color: purple !important; /* Preferably not the same color as the normal header link color. There is limited support for psuedo classes in email clients, this was added just for good measure. */
                    |		}
                    |		html .rtemail h1 {
                    |			font-size: 48px;
                    |			line-height: 1.04167;
                    |			text-transform: none;
                    |			font-weight: 700;
                    |		}
                    |        html .rtemail h2 {
                    |            font-size: 36px;
                    |            line-height: 1.04167;
                    |            text-transform: none;
                    |            font-weight: 700;
                    |        }
                    |        html .rtemail h3 {
                    |            font-size: 24px;
                    |            line-height: 1.04167;
                    |            text-transform: none;
                    |            font-weight: 700;
                    |        }
                    |		html .rtemail p,
                    |		html .rtemail ul li,
                    |		html .rtemail ol li{
                    |			font-family: 'Helvetic Neue', sans-serif, arial;
                    |			font-size: 16px;
                    |			line-height: 1.31579;
                    |			font-weight: 400;
                    |			text-transform: none;
                    |		}
                    |		html .rtemail p.footer{
                    |			font-family: 'Helvetic Neue', sans-serif, arial;
                    |			font-size: 12px;
                    |			line-height: 1.31579;
                    |			font-weight: 400;
                    |			text-transform: none;
                    |		}
                    |		html .rtemail hr{
                    |		    border: 0;
                    |		    height: 0;
                    |		    border-top: 1px solid rgba(0, 0, 0, 0.1);
                    |		    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
                    |		}
                    |
                    |
                    |		/* Outlook 07, 10 Padding issue fix
                    |		Bring inline: No.*/
                    |		html .rtemail table td {border-collapse: collapse;}
                    |
                    |		/* Remove spacing around Outlook 07, 10 tables
                    |		Bring inline: Yes */
                    |		html .rtemail table { border-collapse:collapse; mso-table-lspace:0pt; mso-table-rspace:0pt; }
                    |
                    |		/* Styling your links has become much simpler with the new Yahoo.  In fact, it falls in line with the main credo of styling in email and make sure to bring your styles inline.  Your link colors will be uniform across clients when brought inline.
                    |		Bring inline: Yes. */
                    |		html .rtemail a {color: #2e8aca;}
                    |
                    |
                    |		/***************************************************
                    |		****************************************************
                    |		MOBILE TARGETING
                    |		****************************************************
                    |		***************************************************/
                    |		@media only screen and (max-device-width: 480px) {
                    |			/* Part one of controlling phone number linking for mobile. */
                    |			html .rtemail a[href^="tel"],
                    |			html .rtemail a[href^="sms"] {
                    |						text-decoration: none;
                    |						color: blue; /* or whatever your want */
                    |						pointer-events: none;
                    |						cursor: default;
                    |					}
                    |
                    |			html .rtemail .mobile_link a[href^="tel"],
                    |			html .rtemail .mobile_link a[href^="sms"] {
                    |						text-decoration: default;
                    |						color: orange !important;
                    |						pointer-events: auto;
                    |						cursor: default;
                    |					}
                    |
                    |		}
                    |
                    |		/* More Specific Targeting */
                    |
                    |		@media only screen and (min-device-width: 768px) and (max-device-width: 1024px) {
                    |		/* You guessed it, ipad (tablets, smaller screens, etc) */
                    |			/* repeating for the ipad */
                    |			html .rtemail a[href^="tel"],
                    |			html .rtemail a[href^="sms"] {
                    |						text-decoration: none;
                    |						color: blue; /* or whatever your want */
                    |						pointer-events: none;
                    |						cursor: default;
                    |					}
                    |
                    |			html .rtemail .mobile_link a[href^="tel"],
                    |			html .rtemail .mobile_link a[href^="sms"] {
                    |						text-decoration: default;
                    |						color: orange !important;
                    |						pointer-events: auto;
                    |						cursor: default;
                    |					}
                    |		}
                    |
                    |		@media only screen and (-webkit-min-device-pixel-ratio: 2) {
                    |		/* Put your iPhone 4g styles in here */
                    |		}
                    |
                    |		/* Android targeting */
                    |		@media only screen and (-webkit-device-pixel-ratio:.75){
                    |		/* Put CSS for low density (ldpi) Android layouts in here */
                    |		}
                    |		@media only screen and (-webkit-device-pixel-ratio:1){
                    |		/* Put CSS for medium density (mdpi) Android layouts in here */
                    |		}
                    |		@media only screen and (-webkit-device-pixel-ratio:1.5){
                    |		/* Put CSS for high density (hdpi) Android layouts in here */
                    |		}
                    |		/* end Android targeting */
                    |
                    |	</style>
                    |
                    |	<!-- Targeting Windows Mobile -->
                    |	<!--[if IEMobile 7]>
                    |	<style type="text/css">
                    |
                    |	</style>
                    |	<![endif]-->
                    |
                    |	<!-- ***********************************************
                    |	****************************************************
                    |	END MOBILE TARGETING
                    |	****************************************************
                    |	************************************************ -->
                    |
                    |	<!--[if gte mso 9]>
                    |		<style>
                    |		/* Target Outlook 2007 and 2010 */
                    |		</style>
                    |	<![endif]-->
                    |</head>
                    |<body style="margin: 0; padding: 0; min-width: 100%!important;">
                    |<table cellpadding="0" cellspacing="0" border="0" width="100%" class='rtemail'>
                    |	<tr>
                    |    	<td style="background-color:#0B0C0C;padding: 0 20px 0 20px; height: 50px; width: 100%;" align="center">
                    |        	<table cellpadding="0" cellspacing="0" border="0" style="width: 100%; max-width: 600px;">
                    |				<tr>
                    |                	<td><img class="image_fix" src='https://dev.registered-traveller.homeoffice.gov.uk/assets/images/gov-logo.gif' alt="GOV.UK" title="GOV.UK logo" width="162" height="36" /></td>
                    |                </tr>
                    |            </table>
                    |         </td>
                    |    </tr>
                    |    <tr>
                    |    	<td style="padding: 0 20px 0 20px;" align="center">
                    |        	<table cellpadding="0" cellspacing="0" border="0" style="width: 100%; max-width: 600px;">
                    |        	    <tr>
                    |                    <td style="padding:10px 0 10px 0">&nbsp;</td>
                    |                </tr>
                    |                <tr>
                    |                    <td>
                    |
                    |
                    |    <p>Dear John Smith, </p>
                    |
                    |    <p><strong>Membership number: RTDK8ZPNP</strong></p>
                    |
                    |    <p><strong><a href="http/update-passport/details/A_Update_Token">Apply to update your Registered Traveller membership at http/update-passport/details/A_Update_Token</a> </strong></p>
                    |
                    |    <p>This link will expire within 24 hours. There is a non-refundable £20 charge for changing your passport details.</p>
                    |
                    |    <p>
                    |        <strong>Contact us</strong><br/>
                    |        Contact us at <a href="mailto:RTinbox@homeoffice.gsi.gov.uk">RTinbox@homeoffice.gsi.gov.uk</a> if you are unable to access this link or have any questions. Please include your membership number.<br/>
                    |        We usually reply to enquiries within 10 working days.
                    |    </p>
                    |
                    |
                    |                    </td>
                    |                </tr>
                    |                 <tr>
                    |                    <td>&nbsp;</td>
                    |                </tr>
                    |                <tr>
                    |                    <td  style="padding:5px 0">
                    |                        <p>Yours sincerely,</p>
                    |                        <p>&nbsp;</p>
                    |                        <p><strong>Registered Traveller Team</strong></p>
                    |                    </td>
                    |                </tr>
                    |                <tr>
                    |                    <td style="padding:20px 0 20px 0"><img class="image_fix" src='https://dev.registered-traveller.homeoffice.gov.uk/assets/images/border-force-logo.gif' alt="Border Force" title="Border Force logo" width="160" height="35" /></td>
                    |                </tr>
                    |                <tr>
                    |                	<td style="padding:10px 0 10px 0">&nbsp;</td>
                    |                </tr>
                    |
                    |			    <tr>
                    |			        <td>
                    |			            <p class="footer" style="padding:10px 0 10px 0">This email and any attachments transmitted with it are intended for the named recipient only. If you receive this email in error please notify us immediately and then delete the email and any attachments. It is the responsibility of the recipient to check for the presence of viruses before opening any attachments.</p>
                    |			        </td>
                    |			    </tr>
                    |
                    |            </table>
                    |        </td>
                    |    </tr>
                    |</table>
                    |
                    |<!-- End of wrapper table -->
                    |</body>
                    |</html>
                    |""".stripMargin('|')

}
