package com.cjapps.playinsync;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;

public class PeopleFragment extends Fragment {

    public ListView people_list_view;
    public TextView no_contacts;
    public FastScrollAdapter fastScrollAdapter;
    private Context people_fragment_context;

    private DatabaseManager db_manager;

    public static PeopleFragment newInstance() {
        PeopleFragment fragment = new PeopleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        people_fragment_context = MainActivity.main_context;
        db_manager = new DatabaseManager(people_fragment_context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View main_view = inflater.inflate(R.layout.fragment_people, container, false);
        people_list_view = (ListView)main_view.findViewById(android.R.id.list);

        no_contacts = (TextView)main_view.findViewById(R.id.no_contact_tv);

        initializeAdapter();

        return main_view;
    }

    private void initializeAdapter() {

        people_list_view.setFastScrollEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            people_list_view.setFastScrollAlwaysVisible(true);
        }
        fastScrollAdapter = new FastScrollAdapter(MainActivity.main_context,
                android.R.layout.simple_list_item_1, android.R.id.text1);
        people_list_view.setAdapter(fastScrollAdapter);
    }

    class SimpleAdapter extends ArrayAdapter<Contact> implements PinnedSectionListView.PinnedSectionListAdapter {

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            prepareSections();

            ArrayList<String[]> contact_details = db_manager.getAllContacts();


            int index = 0;
            int count = contact_details.size();

            if(count>0){
                int sectionPosition = 0, listPosition = 0;
                Contact section;
                Contact item;

                char prev = '9';
                char current;

                String name;
                String last_played;
                String dp_url;
                String id;

                while (index<count){

                    id = contact_details.get(index)[0];
                    name = contact_details.get(index)[1];
                    last_played = contact_details.get(index)[2];
                    dp_url = contact_details.get(index)[3];

                    current = name.charAt(0);

                    if(prev != current ) {


                        section = new Contact(Contact.SECTION, current+"", "", "", "");
                        section.sectionPosition = sectionPosition;
                        section.listPosition = listPosition++;
                        onSectionAdded(section, section.sectionPosition);
                        add(section);
                        sectionPosition++;

                        prev = current;
                    }

                    item = new Contact(Contact.CONTACT, name, last_played, dp_url, id);
                    item.sectionPosition = sectionPosition-1;
                    item.listPosition = listPosition++;
                    add(item);

                    index++;
                }
            } else {
                no_contacts.setVisibility(View.VISIBLE);
                people_list_view.setVisibility(View.INVISIBLE);
            }
        }

        protected void prepareSections() { }
        protected void onSectionAdded(Contact section, int sectionPosition) { }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {

            View contact_row = convertView;

            final Contact con = getItem(position);

            if(con.type == Contact.SECTION){

                if(contact_row == null){
                    contact_row = View.inflate(people_fragment_context,R.layout.custom_view_section_header,null);

                    ViewHolder viewHolder = new ViewHolder();

                    viewHolder.section_header = (TextView)contact_row.findViewById(R.id.section_header_tv);
                    contact_row.setTag(viewHolder);
                }

                ViewHolder holder = (ViewHolder) contact_row.getTag();
                holder.section_header.setText(con.name);
                return contact_row;

            } else {

                if(contact_row == null){

                    contact_row = View.inflate(people_fragment_context
                            ,R.layout.custom_view_people_list_item_layout,null);

                    ViewHolder viewHolder = new ViewHolder();

                    viewHolder.name = (TextView)contact_row.findViewById(R.id.people_list_name);
                    viewHolder.last_played = (TextView)contact_row.findViewById(R.id.people_list_last_played);
                    viewHolder.display_pic = (ImageView)contact_row.findViewById(R.id.people_list_dp);

                    contact_row.setTag(viewHolder);

                }

                ViewHolder holder = (ViewHolder) contact_row.getTag();

                holder.name.setText(con.name);
                holder.last_played.setText(con.last_played);

                ImageLoader.MODE = 0;

                MainActivity.song_art_loader.DisplayImage(con.dp_url
                        ,R.drawable.com_facebook_profile_default_icon,holder.display_pic);

                contact_row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent contact_activity = new Intent(people_fragment_context,ContactActivity.class);
                        contact_activity.putExtra("name",con.name);
                        contact_activity.putExtra("last_played",con.last_played);
                        contact_activity.putExtra("dp_url",con.dp_url);
                        startActivity(contact_activity);
                    }
                });

            }

            return contact_row;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Contact.SECTION;
        }


    }

    static class ViewHolder {

        TextView name;
        TextView last_played;
        ImageView display_pic;

        TextView section_header;
    }

    class FastScrollAdapter extends SimpleAdapter implements SectionIndexer {

        private ArrayList<Contact> sections ;

        public FastScrollAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override protected void prepareSections() {
            sections = new ArrayList<Contact>(10);
        }

        @Override protected void onSectionAdded(Contact section, int sectionPosition) {
            sections.add(sectionPosition, section);
        }

        @Override
        public Contact[] getSections() {

            Contact[] temp = new Contact[sections.size()];
            for(int i=0;i<temp.length;i++){
                temp[i] = sections.get(i);
            }
            return temp;
        }

        @Override public int getPositionForSection(int section) {
            if (section >= sections.size()) {
                section = sections.size() - 1;
            }
            return sections.get(section).listPosition;
        }

        @Override public int getSectionForPosition(int position) {
            if (position >= getCount()) {
                position = getCount() - 1;
            }
            return getItem(position).sectionPosition;
        }
    }

    class Contact{

        public static final int CONTACT = 0;
        public static final int SECTION = 1;

        public final int type;

        public String name;
        public String last_played;
        public String dp_url;
        public String id;

        public int sectionPosition;
        public int listPosition;

        public Contact(int type, String name, String last_played, String dp_url, String id){

            this.type = type;
            this.name = name;
            this.last_played = last_played;
            this.dp_url = dp_url;
            this.id = id;

        }

        @Override public String toString() {
            return name;
        }

    }

}


