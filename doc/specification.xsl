<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:freecol="xalan://net.sf.freecol.tools.GenerateDocumentation"
    extension-element-prefixes="freecol">

  <xsl:template match="/">
    <html>
      <head>
        <title>Specification</title>
        <link href="specification.css" rel="stylesheet" type="text/css"/>
      </head>
      <body>
        <xsl:apply-templates select="//goods-types"/>
        <xsl:apply-templates select="//tile-types"/>
        <xsl:apply-templates select="//unit-types"/>
        <xsl:apply-templates select="//building-types"/>
        <xsl:apply-templates select="//founding-fathers"/>
      </body>
    </html>
  </xsl:template>


  <xsl:template name="name">
    <xsl:param name="id"></xsl:param>
    <xsl:param name="key">.image</xsl:param>
    <xsl:variable name="src" select="freecol:getResource(concat($id, $key))"/>
    <td class="name">
      <a id="{$id}">
        <img src="../data/rules/classic/{$src}"/><br />
        <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
      </a>
    </td>
  </xsl:template>


  <xsl:template match="goods-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.TERRAIN.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.goods.initialPrice')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.goods.madeFrom')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="goods-type">
    <tr>
      <xsl:call-template name="name">
        <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
      </xsl:call-template>
      <td>
        <xsl:choose>
          <xsl:when test="market">
            <xsl:value-of select="market/@initial-price"/>
            <xsl:text> / </xsl:text>
            <xsl:value-of select="market/@initial-price + market/@price-difference"/>
          </xsl:when>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="@made-from">
            <xsl:variable name="id" select="@made-from"/>
            <xsl:variable name="src" select="freecol:getResource(concat(@made-from, '.image'))"/>
            <img src="../data/rules/classic/{$src}"/><br />
            <a href="#{$id}">
              <xsl:value-of select="freecol:localize(concat(@made-from, '.name'))"/>
            </a>
          </xsl:when>
        </xsl:choose>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="unit-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.UNITS.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.unit.movement')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.unit.price')"/></th>
        <th><xsl:value-of select="freecol:localize('abilities')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.unit.requirements')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates/>
    </table>
  </xsl:template>

  <xsl:template match="unit-type">
    <xsl:if test="not(@abstract)">
      <tr>
        <xsl:call-template name="name">
          <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
        </xsl:call-template>
        <td>
          <xsl:choose>
            <xsl:when test="@movement">
              <xsl:value-of select="@movement"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>3</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </td>
        <td>
          <xsl:value-of select="@price"/>
        </td>
        <td class="left">
          <xsl:choose>
            <xsl:when test="ability">
              <ul>
                <xsl:apply-templates select="ability"/>
              </ul>
            </xsl:when>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="modifier">
              <ul>
                <xsl:apply-templates select="modifier"/>
              </ul>
            </xsl:when>
          </xsl:choose>
        </td>
        <td class="left">
          <xsl:choose>
            <xsl:when test="required-ability">
              <ul>
                <xsl:apply-templates select="required-ability"/>
              </ul>
            </xsl:when>
          </xsl:choose>
        </td>
        <td class="left">
          <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
        </td>
      </tr>
    </xsl:if>
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
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="tile-type">
    <tr>
      <xsl:choose>
        <xsl:when test="freecol:getResource(concat(@id, '.forest'))">
          <xsl:call-template name="name">
            <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
            <xsl:with-param name="key">.forest</xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="name">
            <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
            <xsl:with-param name="key">.center0.image</xsl:with-param>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
      <td><xsl:value-of select="@basic-move-cost"/></td>
      <td><xsl:value-of select="@basic-work-turns"/></td>
      <td class="left"><xsl:apply-templates select="production[@colonyCenterTile='true']"/></td>
      <td class="left"><xsl:apply-templates select="production[not(@colonyCenterTile='true')]"/></td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
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
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="building-type">
    <tr>
      <xsl:call-template name="name">
        <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
      </xsl:call-template>
      <td>
        <xsl:call-template name="workplaces">
          <xsl:with-param name="building" select="."/>
        </xsl:call-template>
      </td>
      <td><xsl:apply-templates select="production"/></td>
      <td class="left">
        <xsl:choose>
          <xsl:when test="required-goods">
            <xsl:apply-templates select="required-goods"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="freecol:localize('colopedia.buildings.autoBuilt')"/>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
    </tr>
  </xsl:template>


  <xsl:template match="founding-fathers">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.FATHERS.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.birthAndDeath')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.effects')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="founding-father">
    <tr>
      <xsl:call-template name="name">
        <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
      </xsl:call-template>
      <td>
        <xsl:value-of select="freecol:localize(concat(@id, '.birthAndDeath'))"/>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
        <xsl:choose>
          <xsl:when test="ability">
            <ul>
              <xsl:apply-templates select="ability"/>
            </ul>
          </xsl:when>
        </xsl:choose>
        <xsl:choose>
          <xsl:when test="modifier">
            <ul>
              <xsl:apply-templates select="modifier"/>
            </ul>
          </xsl:when>
        </xsl:choose>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.text'))"/>
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
    <xsl:variable name="id" select="@goods-type"/>
    <xsl:value-of select="@value"/>&#160;<a href="#{$id}">
    <xsl:value-of select="freecol:localize(concat(@goods-type, '.name'), '%amount%', string(@value))"/>
    </a><br /><xsl:text> &#8595; </xsl:text><br />
  </xsl:template>

  <xsl:template match="output">
    <xsl:variable name="id" select="@goods-type"/>
    <xsl:value-of select="@value"/>&#160;<a href="#{$id}">
    <xsl:value-of select="freecol:localize(concat(@goods-type, '.name'), '%amount%', string(@value))"/>
    </a><br/>
  </xsl:template>

  <xsl:template match="required-goods">
    <xsl:variable name="id" select="@id"/>
    <xsl:value-of select="@value"/>&#160;<a href="#{$id}">
    <xsl:value-of select="freecol:localize(concat(@id, '.name'), '%amount%', string(@value))"/>
    </a><br/>
  </xsl:template>

  <xsl:template match="ability|required-ability">
    <li>
      <xsl:choose>
        <xsl:when test="@value='false'">
          <strike>
            <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
          </strike>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="scope">
          <br /><xsl:value-of select="freecol:localize('model.scope.name')"/>
          <ul>
            <xsl:apply-templates select="scope"/>
          </ul>
        </xsl:when>
      </xsl:choose>
    </li>
  </xsl:template>

  <xsl:template match="modifier">
    <xsl:variable name="id" select="@id"/>
    <li>
      <xsl:choose>
        <xsl:when test="starts-with($id, 'model.goods.')">
          <a href="#{$id}">
            <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
          </a><xsl:text>:</xsl:text>&#160;
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="freecol:localize(concat($id, '.name'))"/><xsl:text>:</xsl:text>&#160;
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@type='percentage'">
          <xsl:value-of select="@value"/><xsl-text>%</xsl-text>
        </xsl:when>
        <xsl:when test="@type='multiplicative'">
          &#215;<xsl:value-of select="@value"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@value"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="scope">
          <br /><xsl:value-of select="freecol:localize('model.scope.name')"/><xsl:text>:</xsl:text>
          <ul>
            <xsl:apply-templates select="scope"/>
          </ul>
        </xsl:when>
      </xsl:choose>
    </li>
  </xsl:template>

  <xsl:template match="scope">
    <li>
      <xsl:choose>
        <xsl:when test="@match-negated or @ability-value='false'">
          <strike>
            <xsl:call-template name="scope-body"/>
          </strike>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="scope-body"/>
        </xsl:otherwise>
      </xsl:choose>
    </li>
  </xsl:template>

  <xsl:template name="scope-body">
    <xsl:choose>
      <xsl:when test="@type">
        <xsl:variable name="id" select="@type"/>
        <a href="#{$id}">
          <xsl:value-of select="freecol:localize(concat(@type, '.name'))"/>
        </a>
      </xsl:when>
      <xsl:when test="@ability-id">
        <xsl:value-of select="freecol:localize(concat(@ability-id, '.name'))"/>
      </xsl:when>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="workplaces">
    <xsl:param name="building"/>
    <xsl:choose>
      <xsl:when test="$building/@workplaces">
        <xsl:value-of select="$building/@workplaces"/>
      </xsl:when>
      <xsl:when test="$building/@extends">
        <xsl:variable name="parent" select="$building/@extends"/>
        <xsl:call-template name="workplaces">
          <xsl:with-param name="building" select="//building-type[@id=$parent]"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>3</xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>