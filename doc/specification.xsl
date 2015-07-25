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
        <xsl:apply-templates select="//resource-types"/>
        <xsl:apply-templates select="//tile-types"/>
        <xsl:apply-templates select="//unit-types"/>
        <xsl:apply-templates select="//building-types"/>
        <xsl:apply-templates select="//founding-fathers"/>
        <xsl:apply-templates select="//european-nation-types"/>
        <xsl:apply-templates select="//indian-nation-types"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="resource-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.resources.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.effects')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="resource-type">
    <tr>
      <xsl:call-template name="name">
        <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
      </xsl:call-template>
      <td class="left">
        <ul>
          <xsl:apply-templates />
        </ul>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="goods-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.goods.name')"/></h1>
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
    <h1><xsl:value-of select="freecol:localize('colopediaAction.units.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th>
        <xsl:value-of select="freecol:localize('colopedia.unit.movement')"/>
        <xsl:text> / </xsl:text>
        <xsl:value-of select="freecol:localize('model.modifier.offence.name')"/>
        <xsl:text> / </xsl:text>
        <xsl:value-of select="freecol:localize('model.modifier.defence.name')"/>
        </th>
        <th><xsl:value-of select="freecol:localize('colopedia.unit.price')"/></th>
        <th><xsl:value-of select="freecol:localize('abilities')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.unit.requirements')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates/>
    </table>
  </xsl:template>

  <xsl:template match="unit-type">
    <tr>
      <xsl:choose>
        <xsl:when test="@abstract">
          <xsl:attribute name="class">abstract</xsl:attribute>
          <xsl:variable name="id" select="@id"/>
          <td class="name">
            <a id="{$id}">
              <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
            </a>
          </td>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="name">
            <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
      <td>
        <xsl:choose>
          <xsl:when test="@movement">
            <xsl:value-of select="number(@movement) div 3"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>1</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text> / </xsl:text>
        <xsl:choose>
          <xsl:when test="@offence">
            <xsl:value-of select="@offence"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>0</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text> / </xsl:text>
        <xsl:choose>
          <xsl:when test="@defence">
            <xsl:value-of select="@defence"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>1</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:value-of select="@price"/>
      </td>
      <td class="left">
        <xsl:if test="@extends or ability or modifier">
          <ul>
            <xsl:if test="@extends">
              <xsl:variable name="id" select="@extends"/>
              <li>
                <a href="#{$id}"><xsl:value-of select="freecol:localize(concat(@extends, '.name'))"/></a>
              </li>
            </xsl:if>
            <xsl:apply-templates select="ability"/>
            <xsl:apply-templates select="modifier"/>
          </ul>
        </xsl:if>
      </td>
      <td class="left">
        <xsl:if test="required-ability or required-goods">
          <ul>
            <xsl:apply-templates select="required-ability"/>
            <xsl:apply-templates select="required-goods"/>
          </ul>
        </xsl:if>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="tile-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.terrain.name')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.movementCost')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.workTurns')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.colonyCenterTile')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.terrain.production')"/></th>
        <th><xsl:value-of select="freecol:localize('colopediaAction.resources.name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.description')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="tile-type">
    <tr>
      <xsl:call-template name="name">
        <xsl:with-param name="id"><xsl:value-of select="@id"/></xsl:with-param>
        <xsl:with-param name="key">
          <xsl:choose>
            <xsl:when test="@is-forest='true'">.forest</xsl:when>
            <xsl:otherwise>.center0.image</xsl:otherwise>
          </xsl:choose>
        </xsl:with-param>
      </xsl:call-template>
      <td>
        <xsl:value-of select="@basic-move-cost"/>
      </td>
      <td>
        <xsl:value-of select="@basic-work-turns"/>
      </td>
      <td class="left">
        <xsl:apply-templates select="production[@colonyCenterTile='true']"/>
      </td>
      <td class="left">
        <xsl:apply-templates select="production[not(@colonyCenterTile='true')]"/>
      </td>
      <td>
        <xsl:apply-templates select="resource"/>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="building-types">
    <h1><xsl:value-of select="freecol:localize('colopediaAction.buildings.name')"/></h1>
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
        <xsl:call-template name="inherited">
          <xsl:with-param name="element" select="."/>
          <xsl:with-param name="attribute">workplaces</xsl:with-param>
          <xsl:with-param name="default">3</xsl:with-param>
        </xsl:call-template>
      </td>
      <td><xsl:apply-templates select="production"/></td>
      <td class="left">
        <xsl:choose>
          <xsl:when test="required-goods or required-ability">
            <ul>
              <xsl:apply-templates select="required-ability"/>
              <xsl:apply-templates select="required-goods"/>
            </ul>
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
    <h1><xsl:value-of select="freecol:localize('colopediaAction.fathers.name')"/></h1>
    <table>
      <caption>
        <xsl:value-of select="freecol:localize('colopedia.foundingFather.description')"/>
      </caption>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.type')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.birthAndDeath')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.probability')"/></th>
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
        <xsl:value-of select="freecol:localize(concat('model.foundingFather.', @type))"/>
      </td>
      <td>
        <xsl:value-of select="freecol:localize(concat(@id, '.birthAndDeath'))"/>
      </td>
      <td>
        <xsl:value-of select="@weight1"/>
        <xsl:text> / </xsl:text>
        <xsl:value-of select="@weight2"/>
        <xsl:text> / </xsl:text>
        <xsl:value-of select="@weight3"/>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.description'))"/>
        <xsl:if test="ability or modifier or event or unit">
          <ul>
            <xsl:apply-templates select="event"/>
            <xsl:apply-templates select="ability"/>
            <xsl:apply-templates select="modifier"/>
            <xsl:apply-templates select="unit"/>
          </ul>
        </xsl:if>
      </td>
      <td class="left">
        <xsl:value-of select="freecol:localize(concat(@id, '.text'))"/>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="european-nation-types">
    <h1><xsl:value-of select="freecol:localize('colopedia.europeanNationTypes')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('advantage')"/></th>
        <th><xsl:value-of select="freecol:localize('colopediaAction.units.name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopedia.nationType.typeOfSettlements')"/></th>
        <th><xsl:value-of select="freecol:localize('abilities')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="european-nation-type">
    <tr>
      <xsl:variable name="id" select="@id"/>
      <td class="name">
        <a id="{$id}">
          <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
        </a>
      </td>
      <td class="left">
        <ul>
          <xsl:choose>
            <xsl:when test="unit[@id='pioneer']">
              <xsl:apply-templates select="unit[@id='pioneer']"/>
            </xsl:when>
            <xsl:when test="@extends">
              <xsl:variable name="extends" select="@extends"/>
              <xsl:apply-templates select="../european-nation-type[@id=$extends]/unit[@id='pioneer']"/>
            </xsl:when>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="unit[@id='soldier']">
              <xsl:apply-templates select="unit[@id='soldier']"/>
            </xsl:when>
            <xsl:when test="@extends">
              <xsl:variable name="extends" select="@extends"/>
              <xsl:apply-templates select="../european-nation-type[@id=$extends]/unit[@id='soldier']"/>
            </xsl:when>
          </xsl:choose>
          <xsl:choose>
            <xsl:when test="unit[@id='ship']">
              <xsl:apply-templates select="unit[@id='ship']"/>
            </xsl:when>
            <xsl:when test="@extends">
              <xsl:variable name="extends" select="@extends"/>
              <xsl:apply-templates select="../european-nation-type[@id=$extends]/unit[@id='ship']"/>
            </xsl:when>
          </xsl:choose>
        </ul>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="settlement">
            <xsl:value-of select="freecol:localize(concat(settlement/@id, '.name'))"/>
          </xsl:when>
          <xsl:when test="@extends">
            <xsl:variable name="extends" select="@extends"/>
            <xsl:value-of select="freecol:localize(concat(../european-nation-type[@id=$extends]/settlement/@id, '.name'))"/>
          </xsl:when>
        </xsl:choose>
      </td>
      <td class="left">
        <ul>
          <xsl:apply-templates select="ability"/>
          <xsl:apply-templates select="modifier"/>
        </ul>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="indian-nation-types">
    <h1><xsl:value-of select="freecol:localize('colopedia.nativeNationTypes')"/></h1>
    <table>
      <tr>
        <th><xsl:value-of select="freecol:localize('name')"/></th>
        <th><xsl:value-of select="freecol:localize('colopediaAction.skills.name')"/></th>
        <th><xsl:value-of select="freecol:localize('settlement')"/></th>
        <th><xsl:value-of select="freecol:localize('capital')"/></th>
        <th><xsl:value-of select="freecol:localize('abilities')"/></th>
      </tr>
      <xsl:apply-templates />
    </table>
  </xsl:template>

  <xsl:template match="indian-nation-type">
    <tr>
      <xsl:if test="@abstract='true'">
        <xsl:attribute name="class">abstract</xsl:attribute>
      </xsl:if>
      <xsl:variable name="id">
        <xsl:choose>
          <xsl:when test="@abstract='true'">
            <xsl:value-of select="concat('model.settlement.', @id)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@id"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <td class="name">
        <a id="{$id}">
          <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
        </a>
      </td>
      <td class="left">
        <xsl:if test="skill">
          <ul>
            <xsl:apply-templates select="skill"/>
          </ul>
        </xsl:if>
      </td>
      <td class="left">
        <xsl:choose>
          <xsl:when test="settlement">
            <xsl:apply-templates select="settlement[not(@capital='true')]"/>
          </xsl:when>
          <xsl:when test="@extends">
            <xsl:variable name="extends" select="@extends"/>
            <xsl:apply-templates select="../indian-nation-type[@id=$extends]/settlement[not(@capital='true')]"/>
          </xsl:when>
        </xsl:choose>
      </td>
      <td class="left">
        <xsl:choose>
          <xsl:when test="settlement">
            <xsl:apply-templates select="settlement[@capital='true']"/>
          </xsl:when>
          <xsl:when test="@extends">
            <xsl:variable name="extends" select="@extends"/>
            <xsl:apply-templates select="../indian-nation-type[@id=$extends]/settlement[@capital='true']"/>
          </xsl:when>
        </xsl:choose>
      </td>
      <td class="left">
        <xsl:if test="ability or modifier or @extends">
          <ul>
            <xsl:if test="@extends">
              <li>
                <xsl:variable name="id" select="concat('model.settlement.', @extends)"/>
                <a href="#{$id}">
                  <xsl:value-of select="freecol:localize(concat($id, '.name'))"/>
                </a>
              </li>
            </xsl:if>
            <xsl:apply-templates select="ability"/>
            <xsl:apply-templates select="modifier"/>
          </ul>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>


  <xsl:template match="settlement">
    <div class="center">
      <xsl:variable name="src" select="freecol:getResource(concat(@id, '.image'))"/>
      <img src="../data/rules/classic/{$src}"/>
    </div>
    <ul>
      <li>
        <xsl:value-of select="freecol:localize('model.settlement.claimableRadius')"/>:
        <xsl:value-of select="@claimableRadius"/>
      </li>
      <li>
        <xsl:value-of select="freecol:localize('model.settlement.extraClaimableRadius')"/>:
        <xsl:value-of select="@extraClaimableRadius"/>
      </li>
      <li>
        <xsl:value-of select="freecol:localize('model.settlement.minimumSize')"/>:
        <xsl:value-of select="@minimumSize"/>
      </li>
      <li>
        <xsl:value-of select="freecol:localize('model.settlement.maximumSize')"/>:
        <xsl:value-of select="@maximumSize"/>
      </li>
      <xsl:apply-templates select="ability"/>
      <xsl:apply-templates select="modifier"/>
    </ul>
  </xsl:template>

  <xsl:template match="production">
    <xsl:choose>
      <xsl:when test="@production-level">
        <xsl:variable name="production-level" select="@production-level"/>
        <div class="{$production-level}">
          <xsl:apply-templates select="input"/>
          <xsl:apply-templates select="output"/>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <div>
          <xsl:apply-templates select="input"/>
          <xsl:apply-templates select="output"/>
        </div>
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
    <li class="goods">
      <xsl:variable name="id" select="@id"/>
      <xsl:value-of select="@value"/>&#160;<a href="#{$id}">
      <xsl:value-of select="freecol:localize(concat(@id, '.name'), '%amount%', string(@value))"/>
      </a>
    </li>
  </xsl:template>

  <xsl:template match="event">
    <li>
      <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
      <xsl:if test="@value">
        <xsl:text>: </xsl:text>
        <xsl:choose>
          <xsl:when test="number(@value) = @value">
            <xsl:value-of select="@value"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="freecol:localize(concat(@value, '.name'))"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </li>
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

  <xsl:template match="resource">
    <xsl:variable name="id" select="@type"/>
    <a href="#{$id}"><xsl:value-of select="freecol:localize(concat($id, '.name'))"/></a>
    <br />
  </xsl:template>

  <xsl:template match="skill">
    <li>
      <xsl:value-of select="freecol:localize(concat(@id, '.name'))"/>
    </li>
  </xsl:template>


  <xsl:template match="unit">
    <li>
      <xsl:choose>
        <xsl:when test="@type">
          <xsl:if test="@expert-starting-units">
            <xsl:attribute name="class">expert</xsl:attribute>
          </xsl:if>
          <xsl:variable name="id" select="@type"/>
          <a href="#{$id}"><xsl:value-of select="freecol:localize(concat($id, '.name'))"/></a>
          <xsl:if test="@role">
            <xsl:text> / </xsl:text>
            <xsl:value-of select="freecol:localize(concat('model.unit.role.', @role))"/>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:variable name="id" select="@id"/>
          <a href="#{$id}"><xsl:value-of select="freecol:localize(concat($id, '.name'))"/></a>
        </xsl:otherwise>
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
      <xsl:when test="@method-name">
        <xsl:choose>
          <xsl:when test="@method-name='isIndian'">
            <xsl:value-of select="freecol:localize('model.ability.native.name')"/>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="inherited">
    <xsl:param name="element"/>
    <xsl:param name="attribute"/>
    <xsl:param name="default"/>
    <xsl:choose>
      <xsl:when test="$element/@*[name()=$attribute]">
        <xsl:value-of select="$element/@*[name()=$attribute]"/>
      </xsl:when>
      <xsl:when test="$element/@extends">
        <xsl:variable name="parent" select="$element/@extends"/>
        <xsl:call-template name="inherited">
          <xsl:with-param name="element" select="../*[@id=$parent]"/>
          <xsl:with-param name="attribute" select="$attribute"/>
          <xsl:with-param name="default" select="$default"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$default"/></xsl:otherwise>
    </xsl:choose>
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



</xsl:stylesheet>
