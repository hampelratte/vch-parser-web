<#include "header.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<#if PAGE??>
    <div id="tree"></div>

    <script type="text/javascript">
    <!--
        function showDetails(node) {
            $.ajax({
                url: '${SERVLET_URI}',
                type: 'GET',
                data: {
                    id : '${PARSER}',
                    uri : $(node).attr('id')
                },
                dataType: 'json',
                timeout: 30000,
                error: function(xhr, text, error) {
                    $('#content').html('');
                    var output_text = '<strong>' + xhr.status + ' - ' + xhr.statusText + '</strong>';
                    if(!xhr.responseText.indexOf('<html>') == 0) {
                        output_text += '<br/>' + xhr.responseText;
                    }
                    $.pnotify( {
                        pnotify_title : '${I18N_ERROR}',
                        pnotify_text : output_text,
                        pnotify_type : 'error',
                        pnotify_hide: false
                    });
                },
                success: function(response){
                    var video = response.video.data;
                    var attributes = response.video.attributes;
                    var actions = response.actions;
                    
                    var html = '<h1>' + video.title + '</h1>';
                    
                    // add a preview, if available
                    if(attributes.vchthumb) {
                        html += '<p><img src="'+attributes.vchthumb+'" alt="Preview" class="thumb ui-widget-content ui-corner-all"/></p>';
                    }
                    
                    html += '<p><strong>';
                    
                    // add the pubdate, if available
                    if(attributes.vchpubDate) {
                        var date = new Date();
                        date.setTime(attributes.vchpubDate);
                        html += date.toLocaleString();
                    }
                    
                    if(attributes.vchpubDate && attributes.vchduration) {
                        html += ' - ';                    
                    }
                    
                    // add the duration, if available
                    if(attributes.vchduration) {
                        var secs = parseInt(attributes.vchduration);
                        if(secs < 60) {
                            html += secs + ' ${I18N_SECONDS}';                        
                        } else {
                            var minutes = Math.floor(secs / 60);
                            if(minutes < 10) minutes = "0"+minutes;
                            var secs = secs % 60;
                            if(secs < 10) secs = "0"+secs;
                            html += minutes+':'+secs + ' ${I18N_MINUTES}'; 
                        }
                    }
                    
                    html += '</strong></p>';
                    
                    // add the description, if available
                    if(attributes.vchdesc) {
                        html += '<p>' + attributes.vchdesc + '</p>';
                    } 
                    
                    // add web actions
                    if(attributes.vchvideo && actions) {
                        for(var i=0; i<actions.length; i++) {
                            html += '<a style="margin-right: 1em;" id="action'+i+'" href="'+actions[i].uri+'">'+actions[i].title+'</a>';
                        }
                    }
                    
                    // display the details
                    $('#content').html(html);
                    if(attributes.vchvideo) {
                        $('#watch').button( {icons: { primary: 'ui-icon-play'}} );
                        if(actions) {
                            for(var i=0; i<actions.length; i++) {
                                $('#action'+i).button();           
                            }
                        }
                    }
                    $('#open').button( {icons: { primary: 'ui-icon-extlink'}} );
                }
            });
        }
        
        
        $(document).ready(function() {
            var stat =  [{ 
                attributes : { 
                    id : "${PAGE.vchUri}" }, 
                data: { 
                    title : "${TITLE}", 
                    icon : "" }, 
                state : "closed"
            }];
            
            $(function () { 
                $("#tree").tree({
                    data : { 
                        type : "json",
                        async : true,
                        opts : {
                            async : true,
                            method : "POST",
                            url : "${SERVLET_URI}"
                        }
                    },
                    callback : { 
                        // Make sure static is not used once the tree has loaded for the first time
                        onload : function (t) { 
                            t.settings.data.opts.static = false; 
                        },
                        // Take care of refresh calls - n will be false only when the whole tree is refreshed or loaded of the first time
                        beforedata : function (n, t) { 
                            if(n == false) { t.settings.data.opts.static = stat; }
                            return {
                                id : "${PARSER}", 
                                uri : $(n).attr("id")
                            };
                        },
                        onselect : function(n, t) { 
                            if($(n).attr("vchisleaf")) {
                                $('#content').html('<h1>${I18N_LOADING}</h1><img src="/static/icons/loadingAnimation.gif" alt=""/>');
                                showDetails(n);
                            }
                        },
                        error : function(text, tree) {
                            if(text.indexOf("DESELECT") >= 0) return;
                            
                            $.pnotify( {
                                pnotify_title : '${I18N_ERROR}',
                                pnotify_text : text,
                                pnotify_type : 'error',
                                pnotify_hide: false
                            });
                        }
                    },
                    ui : {
                        theme_name : "themeroller",
                        dots : false,
                        animation : 500
                    },
                    lang : {
                        loading: '${I18N_LOADING}'
                    },
                    plugins : {
                        themeroller : { }
                    }
                });
            });
        });
    // -->
    </script>
    
    <div id="content"></div>
</#if>
<#include "footer.ftl">