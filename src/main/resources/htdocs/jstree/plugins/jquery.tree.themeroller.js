(function ($) {
	$.extend($.tree.plugins, {
		"themeroller" : {
			defaults : {

			},
			callbacks : {
				oninit : function (t) {
					if(this.settings.ui.theme_name != "themeroller") return;
					var opts = $.extend(true, {}, $.tree.plugins.themeroller.defaults, this.settings.plugins.themeroller);
					this.container.addClass("ui-widget-content");
                    this.container.css("border", "0");
					$("#" + this.container.attr("id") + " li a").live("mouseover", function () { $(this).addClass("ui-state-hover"); });
					$("#" + this.container.attr("id") + " li a").live("mouseout",  function () { $(this).removeClass("ui-state-hover"); });
				},
				onparse : function (s, t) {
					if(this.settings.ui.theme_name != "themeroller") return;
					var opts = $.extend(true, {}, $.tree.plugins.themeroller.defaults, this.settings.plugins.themeroller);
					return $(s).find("a").not(".ui-corner-all").addClass("ui-corner-all ui-state-default").children("ins").remove().end().end().end();
					//return $(s).find("a").not(".ui-corner-all").addClass("ui-corner-all ui-state-default").children("ins").addClass("ui-icon ui-icon-triangle-1-e").end().end().end();
				},
				onselect : function(n, t) {
					if(this.settings.ui.theme_name != "themeroller") return;
					var opts = $.extend(true, {}, $.tree.plugins.themeroller.defaults, this.settings.plugins.themeroller);
					$(n).children("a").addClass("ui-state-active");
				},
				ondeselect : function(n, t) {
					if(this.settings.ui.theme_name != "themeroller") return;
					var opts = $.extend(true, {}, $.tree.plugins.themeroller.defaults, this.settings.plugins.themeroller);
					$(n).children("a").removeClass("ui-state-active");
				}
			}
		}
	});
})(jQuery);