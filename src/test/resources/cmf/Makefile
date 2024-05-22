CT = ../../../../build/install/cmftool/bin/cmftool
NIEM6 = \
  augment.cmf \
  cli.cmf \
  clsa.cmf \
  complexContent.cmf \
  components.cmf \
  datatypes.cmf \
  externals.cmf \
  globalAug-1.cmf \
  globalAug-2.cmf \
  isRefAtt.cmf \
  localTerm.cmf \
  proxy.cmf \
  relProp.cmf \
  union.cmf \

all : $(NIEM5) $(NIEM6)

complexContent5.cmf: ../xsd5/complexContent.xsd
	$(CT) x2m $< > $@

$(NIEM6): %.cmf: ../xsd6/%.xsd
	$(CT) x2m $< > $@

clean :
	rm $(NIEM5) $(NIEM6)
