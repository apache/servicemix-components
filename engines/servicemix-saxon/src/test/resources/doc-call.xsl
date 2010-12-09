<?xml version="1.0" encoding="ISO-8859-1"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:d="http://dccodes.data" xmlns:f="http://faccodes.data" version="1.0">
	<xsl:output method="text"/>
	<xsl:variable name="beginHour" select="HH"/>
	<xsl:variable name="beginMin" select="MM"/>
	<xsl:variable name="GlobalbeginMin" select="0"/>
	<xsl:key name="dccode-lookup" match="d:dccode" use="d:dcfac"/>
	<xsl:variable name="dccodes-top" select="document('')/*/d:dccodes"/>
	<xsl:key name="faccode-lookup" match="f:faccode" use="f:fac"/>
	<xsl:variable name="faccodes-top" select="document('')/*/f:faccodes"/>
	<xsl:template match="/">
	<xsl:text><![CDATA[<data>]]></xsl:text>
	<xsl:text>&#xa;</xsl:text>
		<xsl:for-each select="Command/Execute/Parms/Output/parmDocMonAppointment/DocMonAppointment">
			<!--Default Appointment record ID -->
			<text>RT</text>
			<!-- Add/Modify/Delete transformation -->
			<xsl:call-template name="append-pad">
				<xsl:with-param name="padChar">&#160;</xsl:with-param>
				<xsl:with-param name="padVar" select="translate(@ActionCode,'AMD','012')"/>
				<xsl:with-param name="length" select="1"/>
			</xsl:call-template>
			<!-- Owner-ID -->
			<xsl:apply-templates select="$dccodes-top">
				<xsl:with-param name="curr-label" select="MonAppointmentApptLoc[@PartyType='1']/@FacilityCode"/>
			</xsl:apply-templates>
			<xsl:apply-templates select="$faccodes-top">
				<xsl:with-param name="curr-fac-label" select="MonAppointmentApptLoc[@PartyType='1']/@FacilityCode"/>
			</xsl:apply-templates>
			<!-- Appointment number -->
			<xsl:call-template name="prepend-pad">
				<xsl:with-param name="padChar">0</xsl:with-param>
				<xsl:with-param name="padVar" select="./@RequestNumber"/>
				<xsl:with-param name="length" select="10"/>
			</xsl:call-template>
			<!-- SCAC Code -->
			<xsl:call-template name="append-pad">
				<xsl:with-param name="padChar" select="' '" />
				<xsl:with-param name="padVar" select="MonAppointmentApptLoc[@PartyType='2']/@PartyCode"/>
				<xsl:with-param name="length" select="12"/>
			</xsl:call-template>
			<!-- Trucker Date -->
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="../DocMonAppointment/@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="0"/>
				<xsl:with-param name="length" select="5"/>
			</xsl:call-template>
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="6"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="9"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<!-- PO number -->
			<xsl:call-template name="prepend-pad">
				<xsl:with-param name="padChar">0</xsl:with-param>
				<xsl:with-param name="padVar" select="DocAppointmentLine/@PONumber"/>
				<xsl:with-param name="length" select="18"/>
			</xsl:call-template>
			<!-- number of cases-->
			<xsl:call-template name="prepend-pad">
				<xsl:with-param name="padChar">0</xsl:with-param>
				<xsl:with-param name="padVar" select="DocAppointmentLine/@NumCasesShipped"/>
				<xsl:with-param name="length" select="5"/>
			</xsl:call-template>
			<!-- Trucker Date -->
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="../DocMonAppointment/@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="0"/>
				<xsl:with-param name="length" select="5"/>
			</xsl:call-template>
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="6"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<xsl:call-template name="strip-date">
				<xsl:with-param name="wholeDate" select="@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="9"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<!-- Receiving door-->
			<xsl:call-template name="prepend-pad">
				<xsl:with-param name="padChar">0</xsl:with-param>
				<xsl:with-param name="padVar" select="MonAppointmentApptDoor/@DoorNum"/>
				<xsl:with-param name="length" select="3"/>
			</xsl:call-template>
			<!-- Appointment begin time HHMM-->
			<xsl:call-template name="strip-hour">
				<xsl:with-param name="wholeDate" select="./@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="12"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<xsl:call-template name="strip-min">
				<xsl:with-param name="wholeDate" select="./@AppointmentDTBegin"/>
				<xsl:with-param name="beginAt" select="15"/>
				<xsl:with-param name="length" select="2"/>
			</xsl:call-template>
			<xsl:call-template name="concat-hhmm">
				<xsl:with-param name="beginHour" select="substring(./@AppointmentDTBegin,12,2)"/>
				<xsl:with-param name="beginMin" select="substring(./@AppointmentDTBegin,15,2)  + 30 "/>
			</xsl:call-template>
			<!-- Carrige Return-->
			<xsl:text>&#xa;</xsl:text>
		</xsl:for-each>
	<xsl:text><![CDATA[</data>]]></xsl:text>
	</xsl:template>
	<xsl:template name="append-pad">
		<!-- recursive template to left justify and append  -->
		<!-- the value with whatever padChar is passed in   -->
		<xsl:param name="padChar"> </xsl:param>
		<xsl:param name="padVar"/>
		<xsl:param name="length"/>
		<xsl:choose>
			<xsl:when test="string-length($padVar) &lt; $length">
				<xsl:call-template name="append-pad">
					<xsl:with-param name="padChar" select="$padChar"/>
					<xsl:with-param name="padVar" select="concat($padVar,$padChar)"/>
					<xsl:with-param name="length" select="$length"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="substring($padVar,1,$length)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="d:dccodes">
		<xsl:param name="curr-label"/>
		<xsl:value-of select="key('dccode-lookup', $curr-label)/d:trancode"/>
	</xsl:template>
	<xsl:template name="prepend-pad">
		<!-- recursive template to right justify and prepend-->
		<!-- the value with whatever padChar is passed in   -->
		<xsl:param name="padChar"> </xsl:param>
		<xsl:param name="padVar"/>
		<xsl:param name="length"/>
		<xsl:choose>
			<xsl:when test="string-length($padVar) &lt; $length">
				<xsl:call-template name="prepend-pad">
					<xsl:with-param name="padChar" select="$padChar"/>
					<xsl:with-param name="padVar" select="concat($padChar,$padVar)"/>
					<xsl:with-param name="length" select="$length"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="substring($padVar,string-length($padVar) - $length + 1)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!--  strip the digits of the date -->
	<xsl:template name="strip-date">
		<xsl:param name="wholeDate"/>
		<xsl:param name="beginAt"/>
		<xsl:param name="length"/>
		<xsl:value-of select="substring($wholeDate,$beginAt,$length)"/>
	</xsl:template>
	<!--  strip the hour digits  -->
	<xsl:template name="strip-hour">
		<xsl:param name="wholeDate"/>
		<xsl:param name="beginAt"/>
		<xsl:param name="length"/>
		<xsl:value-of select="substring($wholeDate,$beginAt,$length)"/>
	</xsl:template>
	<!--  strip the minutes digits  -->
	<xsl:template name="strip-min">
		<xsl:param name="wholeDate"/>
		<xsl:param name="beginAt"/>
		<xsl:param name="length"/>
		<xsl:value-of select="substring($wholeDate,$beginAt,$length)"/>
	</xsl:template>
	<xsl:template name="concat-hhmm">
		<xsl:param name="beginHour"/>
		<xsl:param name="beginMin"/>
		<xsl:choose>
			<xsl:when test="$beginMin &gt; 59">
				<xsl:value-of select="concat($beginHour +1,$GlobalbeginMin,$GlobalbeginMin)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat($beginHour,$beginMin)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
    <!-- Table to translate DC Facility to Owner-Id -->
    <d:dccodes>
        <d:dccode>
			<d:trancode>A07</d:trancode>
			<d:dcfac>ALA1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>A07</d:trancode>
			<d:dcfac>ALA3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>C13</d:trancode>
			<d:dcfac>CRC1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>C13</d:trancode>
			<d:dcfac>CRC2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>C13</d:trancode>
			<d:dcfac>CRC3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>C13</d:trancode>
			<d:dcfac>CRC4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>C13</d:trancode>
			<d:dcfac>CRC7</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>D10</d:trancode>
			<d:dcfac>DTD1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>D10</d:trancode>
			<d:dcfac>DTD2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>D10</d:trancode>
			<d:dcfac>DTD3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>E09</d:trancode>
			<d:dcfac>ENE1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>E09</d:trancode>
			<d:dcfac>ENE2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>E09</d:trancode>
			<d:dcfac>ENE3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>E09</d:trancode>
			<d:dcfac>ENE7</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>F11</d:trancode>
			<d:dcfac>FLF1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>F11</d:trancode>
			<d:dcfac>FLF2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>F11</d:trancode>
			<d:dcfac>FLF7</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>I02</d:trancode>
			<d:dcfac>INI1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>I02</d:trancode>
			<d:dcfac>INI2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>I02</d:trancode>
			<d:dcfac>INI3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>I02</d:trancode>
			<d:dcfac>INI4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>L15</d:trancode>
			<d:dcfac>LAL1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>L15</d:trancode>
			<d:dcfac>LAL2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>L15</d:trancode>
			<d:dcfac>LAL3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM5</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM6</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM8</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>M05</d:trancode>
			<d:dcfac>MAM9</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN7</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN8</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>N04</d:trancode>
			<d:dcfac>NJN9</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>O12</d:trancode>
			<d:dcfac>ORO1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>O12</d:trancode>
			<d:dcfac>ORO2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>O12</d:trancode>
			<d:dcfac>ORO3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>O12</d:trancode>
			<d:dcfac>ORO4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>O12</d:trancode>
			<d:dcfac>ORO5</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>P03</d:trancode>
			<d:dcfac>PAP1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>P03</d:trancode>
			<d:dcfac>PAP3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>S08</d:trancode>
			<d:dcfac>SCS1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>S08</d:trancode>
			<d:dcfac>SCS3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>T06</d:trancode>
			<d:dcfac>TNT1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>T06</d:trancode>
			<d:dcfac>TNT2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>T06</d:trancode>
			<d:dcfac>TNT3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>T06</d:trancode>
			<d:dcfac>TNT9</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW1</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW2</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW3</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW4</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW5</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW6</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW7</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW8</d:dcfac>
		</d:dccode>
		<d:dccode>
			<d:trancode>W01</d:trancode>
			<d:dcfac>WNW9</d:dcfac>
		</d:dccode>
	</d:dccodes>
	<!-- Table to translate DC Facility to Fafility Number -->
	<xsl:template match="f:faccodes">
		<xsl:param name="curr-fac-label"/>
		<xsl:value-of select="key('faccode-lookup', $curr-fac-label)/f:trancode"/>
	</xsl:template>
	<f:faccodes>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>ALA1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>ALA3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>CRC1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>CRC2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>CRC3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>CRC7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>07</f:trancode>
			<f:fac>CRC7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>DTD1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>DTD2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>DTD3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>ENE1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>ENE2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>ENE3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>07</f:trancode>
			<f:fac>ENE7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>FLF1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>FLF2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>07</f:trancode>
			<f:fac>FLF7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>INI1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>INI2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>INI3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>INI4</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>LAL1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>LAL2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>LAL3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>MAM1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>MAM2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>MAM3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>MAM4</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>05</f:trancode>
			<f:fac>MAM5</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>06</f:trancode>
			<f:fac>MAM6</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>08</f:trancode>
			<f:fac>MAM8</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>09</f:trancode>
			<f:fac>MAM9</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>NJN1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>NJN20</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>NJN3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>NJN4</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>07</f:trancode>
			<f:fac>NJN7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>08</f:trancode>
			<f:fac>NJN8</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>09</f:trancode>
			<f:fac>NJN9</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>ORO1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>ORO2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>ORO3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>ORO4</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>05</f:trancode>
			<f:fac>ORO5</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>PAP1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>PAP2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>SCS1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>SCS2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>TNT1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>TNT2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>TNT3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>09</f:trancode>
			<f:fac>TNT9</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>01</f:trancode>
			<f:fac>WNW1</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>02</f:trancode>
			<f:fac>WNW2</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>03</f:trancode>
			<f:fac>WNW3</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>04</f:trancode>
			<f:fac>WNW4</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>05</f:trancode>
			<f:fac>WNW5</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>06</f:trancode>
			<f:fac>WNW6</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>07</f:trancode>
			<f:fac>WNW7</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>08</f:trancode>
			<f:fac>WNW8</f:fac>
		</f:faccode>
		<f:faccode>
			<f:trancode>09</f:trancode>
			<f:fac>WNW9</f:fac>
		</f:faccode>
	</f:faccodes>
</xsl:transform>
