package com.legion.web;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.legion.net.entities.LegionNodeContext;
import com.legion.net.entities.LegionNodeInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class StatsController {
    private final static Gson GSON = new Gson();

    @GetMapping("/gms_stats")
    public String getGmsStats() {
        return LegionNodeContext.context().toString();
    }

    @GetMapping("/")
    public String index(Model model) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo info = context.getSelfInfo();
        model.addAttribute("selfInfo", info);
        model.addAttribute("groups", context.getGroupAddress());
        model.addAttribute("metas", context.getClusterMeta().getAllEndpoints());
        model.addAttribute("clusters", context.getClusterNodes().values());
        return "index";
    }

    @RequestMapping({"/index", "index.html"})
    public String statistic(Model model) {
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo info = context.getSelfInfo();
        model.addAttribute("selfInfo", info);
        model.addAttribute("global", context.getClusterNodes());
        model.addAttribute("meta", context.getClusterMeta());

        // model.addAttribute("code", "3002");
        return "statistics";
    }

    @ResponseBody
    @RequestMapping("/statistic")
    public String statistic() {
        Map<String, Object> data = Maps.newHashMap();
        LegionNodeContext context = LegionNodeContext.context();
        LegionNodeInfo info = context.getSelfInfo();
        data.put("selfInfo", info);
        data.put("groupsAddr", context.getGroupAddress());
        data.put("metas", context.getClusterMeta().getAllEndpoints());
        data.put("clusters", context.getClusterNodes().values());
        return GSON.toJson(data);
    }
}
