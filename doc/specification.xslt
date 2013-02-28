<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:freecol="http://www.freecol.org">

  <xsl:variable name="resources" select="document('resources.xml')"/>
  <xsl:variable name="translations" select="document('freecol.tmx')"/>
  <xsl:variable name="lang">en</xsl:variable>

  <xsl:function name="freecol:localize">
    <xsl:param name="id"/>
    <xsl:param name="lang"/>
    <xsl:value-of select="$translations//tu[@tuid=concat($id, '.name')]/tuv[@xml:lang=$lang]/seg"/>
  </xsl:function>

  <xsl:function name="freecol:resource">
    <xsl:param name="id"/>
    <xsl:value-of select="$resources//entry[@key=concat($id, '.center0.image')]/text()"/>
  </xsl:function>

  <xsl:template match="/">
    <html>
      <head>
        <title>Specification</title>
      </head>
      <body>
        <xsl:apply-templates />
      </body>
    </html>
  </xsl:template>

  <xsl:template match="tile-types">
    <h1>Tile Types</h1>
    <table>
      <tr>
        <th>Name</th>
        <th>Move cost</th>
        <th>Work turns</th>
        <th>Center tile</th>
        <th>Production</th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="tile-type">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="src" select="freecol:resource($id)"/>
    <tr>
      <td>
        <img src="data/rules/classic/{$src}"/>
        <xsl:value-of select="freecol:localize($id, $lang)"/>
      </td>
      <td><xsl:value-of select="@basic-move-cost"/></td>
      <td><xsl:value-of select="@basic-work-turns"/></td>
      <td><xsl:apply-templates select="production[@colonyCenterTile='true']"/></td>
      <td><xsl:apply-templates select="production[not(@colonyCenterTile='true')]"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="production">
    <xsl:apply-templates select="output"/>
  </xsl:template>

  <xsl:template match="output">
    <xsl:value-of select="@value"/><xsl:text> </xsl:text>
    <xsl:value-of select="freecol:localize(@goods-type, $lang)"/>
    <br/>
  </xsl:template>

</xsl:stylesheet>