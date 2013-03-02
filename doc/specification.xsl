<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:freecol="xalan://net.sf.freecol.tools.GenerateDocumentation"
    extension-element-prefixes="freecol">
<!--
  <xsl:variable name="resources" select="document('resources.xml')"/>
  <xsl:variable name="translations" select="document('freecol.tmx')"/>
  <xsl:variable name="lang">en</xsl:variable>
-->

  <xsl:template match="/">
    <html>
      <head>
        <title>Specification</title>
        <link href="specification.css" rel="stylesheet" type="text/css"/>
      </head>
      <body>
        <xsl:apply-templates select="//tile-types"/>
        <xsl:apply-templates select="//building-types"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="tile-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.TERRAIN.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.movementCost')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.workTurns')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.colonyCenterTile')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.production')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="tile-type">
    <tr>
      <td class="name">
        <xsl:variable name="src" select="freecol:getResource(concat(@id, '.center0.image'))"/>
        <img src="../data/rules/classic/{$src}"/><br />
        <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
      </td>
      <td><xsl:value-of select="@basic-move-cost"/></td>
      <td><xsl:value-of select="@basic-work-turns"/></td>
      <td><xsl:apply-templates select="production[@colonyCenterTile='true']"/></td>
      <td><xsl:apply-templates select="production[not(@colonyCenterTile='true')]"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="building-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.BUILDINGS.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.buildings.workplaces')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.buildings.production')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.buildings.requires')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="building-type">
    <tr>
      <td class="name">
        <xsl:variable name="src" select="freecol:getResource(concat(@id, '.image'))"/>
        <img src="../data/rules/classic/{$src}"/><br />
        <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
      </td>
      <td>
        <xsl:value-of select="@workplaces"/>
        <!-- TODO: consider inheritance
        <xsl:choose>
          <xsl:when test="@workplaces"><xsl:value-of select="@workplaces"/></xsl:when>
          <xsl:otherwise>3</xsl:otherwise>
        </xsl:choose>
        -->
      </td>
      <td><xsl:apply-templates select="production"/></td>
      <td>
        <xsl:apply-templates select="required-goods"/>
      </td>
    </tr>
  </xsl:template>


  <xsl:template match="production">
    <xsl:choose>
      <xsl:when test="@productionLevel">
        <xsl:variable name="productionLevel" select="@productionLevel"/>
        <span class="{$productionLevel}">
          <xsl:apply-templates select="input"/>
          <xsl:apply-templates select="output"/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <span>
          <xsl:apply-templates select="input"/>
          <xsl:apply-templates select="output"/>
        </span>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="input">
    <xsl:value-of select="@value"/><xsl:text> </xsl:text>
    <xsl:value-of select="freecol:localize(concat(@goods-type, '.name'), '%amount%', string(@value))"/>
    <xsl:text> &#8594; </xsl:text>
  </xsl:template>

  <xsl:template match="output">
    <xsl:value-of select="@value"/><xsl:text> </xsl:text>
    <xsl:value-of select="freecol:localize(concat(@goods-type, '.name'), '%amount%', string(@value))"/>
    <br/>
  </xsl:template>

  <xsl:template match="required-goods">
    <xsl:value-of select="@value"/><xsl:text> </xsl:text>
    <xsl:value-of select="freecol:localize(concat(@id, '.name'), '%amount%', string(@value))"/>
    <br/>
  </xsl:template>


</xsl:stylesheet>